package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Role;
import com.fashion.ecommerce.domain.model.User;
import com.fashion.ecommerce.domain.repository.RefreshTokenStore;
import com.fashion.ecommerce.domain.repository.RoleRepository;
import com.fashion.ecommerce.domain.repository.UserRepository;
import com.fashion.ecommerce.infrastructure.security.JwtTokenProvider;
import com.fashion.ecommerce.presentation.dto.AdminUserDTO;
import com.fashion.ecommerce.presentation.dto.AuthResponse;
import com.fashion.ecommerce.presentation.dto.LoginRequest;
import com.fashion.ecommerce.presentation.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Admin authentication: login (access + refresh pair), refresh rotation with
 * jti single-use detection, and logout (jti revocation).
 * <p>
 * The refresh flow is intentionally strict: a token that is not a refresh
 * token, is expired, was already consumed (reuse) or whose owner no longer
 * exists all map to 401 — never a user-enumeration difference.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not configured"));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(customerRole)
                .isActive(true)
                .isVerified(true)
                .build();
        userRepository.save(user);
        log.info("Registered new customer: email={}", request.email());

        return login(new LoginRequest(request.email(), request.password()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = issueRefreshToken(user);
        log.info("Login successful: email={}", user.getEmail());

        return AuthResponse.of(accessToken, refreshToken,
                tokenProvider.getAccessTokenExpirationMs(), toUserDto(user));
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }

        String jti = tokenProvider.getJtiFromToken(refreshToken);
        if (!refreshTokenStore.consume(jti)) {
            // The jti was already consumed by a previous refresh (reuse) or
            // never issued: reject with a generic 401.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse detected");
        }

        String email = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid or expired refresh token"));

        String accessToken = tokenProvider.generateAccessToken(buildAuthentication(user));
        String newRefreshToken = issueRefreshToken(user);
        log.info("Refresh successful: email={}", user.getEmail());

        return AuthResponse.of(accessToken, newRefreshToken,
                tokenProvider.getAccessTokenExpirationMs(), toUserDto(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            return; // Nothing to revoke for an invalid/expired token — 204.
        }
        refreshTokenStore.revoke(tokenProvider.getJtiFromToken(refreshToken));
        log.info("Logout: refresh token revoked jti={}", tokenProvider.getJtiFromToken(refreshToken));
    }

    private String issueRefreshToken(User user) {
        String refreshToken = tokenProvider.generateRefreshToken(buildAuthentication(user));
        refreshTokenStore.store(tokenProvider.getJtiFromToken(refreshToken), user.getEmail());
        return refreshToken;
    }

    /**
     * Rebuilds an Authentication for the (DB-loaded) user so tokens can be
     * issued after the password check without touching the AuthenticationManager.
     */
    private Authentication buildAuthentication(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + roleName(user)));
        return new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
    }

    private String roleName(User user) {
        return user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
    }

    private AdminUserDTO toUserDto(User user) {
        return new AdminUserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                List.of(roleName(user)));
    }
}
