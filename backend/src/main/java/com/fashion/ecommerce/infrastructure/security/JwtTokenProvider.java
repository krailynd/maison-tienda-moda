package com.fashion.ecommerce.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Canonical JWT token provider (deduplicated, see design D4).
 * <ul>
 *   <li>HS512 (HMAC-SHA512) signing.</li>
 *   <li>Refresh tokens carry a unique JTI for rotation / reuse detection.</li>
 *   <li>Fail-fast at startup when the configured secret is shorter than 64 bytes
 *       (HS512 requires a 512-bit key).</li>
 * </ul>
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /** HS512 requires a key of at least 512 bits (64 bytes). */
    public static final int MIN_SECRET_BYTES = 64;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:900000}") // 15 minutes default
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 days default
    private long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set JWT_SECRET (app.jwt.secret).");
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES + " bytes for HS512, "
                            + "but the configured value has " + secretBytes.length + " bytes.");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        log.info("JWT Token Provider initialized with HS512 (secret {} bytes)", secretBytes.length);
    }

    /**
     * Generates a short-lived access token carrying the user's authorities.
     */
    public String generateAccessToken(Authentication authentication) {
        String username = resolveUsername(authentication);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("type", "access");

        return createToken(claims, username, jwtExpirationMs);
    }

    /**
     * Generates a long-lived refresh token with a unique JTI for rotation.
     */
    public String generateRefreshToken(Authentication authentication) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());

        return createToken(claims, resolveUsername(authentication), refreshExpirationMs);
    }

    /**
     * Extracts the subject (username/email) from a token.
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extracts the roles claim from an access token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object roles = claims.get("roles");
            return roles instanceof List ? (List<String>) roles : List.of();
        });
    }

    /**
     * Stateless validation: signature, expiration and token type (access only).
     * Never touches the database (design D3).
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean isAccess = "access".equals(claims.get("type", String.class));
            return notExpired && isAccess;
        } catch (Exception e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh-token validation: signature, expiration and token type
     * ({@code type=refresh}). Access tokens are rejected because they carry
     * {@code type=access}. Validation is stateless — single-use / rotation
     * state lives in the {@link com.fashion.ecommerce.domain.repository.RefreshTokenStore}.
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean isRefresh = "refresh".equals(claims.get("type", String.class));
            return notExpired && isRefresh;
        } catch (Exception e) {
            log.debug("Invalid refresh JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the JTI claim for refresh-token reuse detection.
     */
    public String getJtiFromToken(String token) {
        return getClaimFromToken(token, Claims::getId);
    }

    /**
     * Access-token lifetime in milliseconds (used for the login {@code expiresIn}
     * field so clients can schedule silent refreshes).
     */
    public long getAccessTokenExpirationMs() {
        return jwtExpirationMs;
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(getAllClaimsFromToken(token));
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String resolveUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return String.valueOf(principal);
    }
}
