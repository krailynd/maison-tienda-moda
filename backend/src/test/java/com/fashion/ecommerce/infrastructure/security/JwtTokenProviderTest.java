package com.fashion.ecommerce.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the canonical {@link JwtTokenProvider} (T-S1-004):
 * HS512 token round-trip and the fail-fast short-secret guard.
 */
class JwtTokenProviderTest {

    private static final String VALID_SECRET = "dev-secret-key-change-in-production-minimum-256-bits-long-for-security"; // 70 bytes

    private JwtTokenProvider providerWithSecret(String secret) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 900_000L);
        ReflectionTestUtils.setField(provider, "refreshExpirationMs", 604_800_000L);
        provider.init();
        return provider;
    }

    @Test
    void accessTokenRoundTrip() {
        JwtTokenProvider provider = providerWithSecret(VALID_SECRET);

        User principal = new User("cliente@example.com", "x",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = provider.generateAccessToken(auth);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("cliente@example.com");
        assertThat(provider.getRolesFromToken(token)).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void refreshTokenCarriesJti() {
        JwtTokenProvider provider = providerWithSecret(VALID_SECRET);

        User principal = new User("cliente@example.com", "x", List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String refreshToken = provider.generateRefreshToken(auth);

        assertThat(refreshToken).isNotBlank();
        assertThat(provider.getJtiFromToken(refreshToken)).isNotBlank();
    }

    @Test
    void validateRefreshTokenAcceptsRefreshAndRejectsAccess() {
        JwtTokenProvider provider = providerWithSecret(VALID_SECRET);

        User principal = new User("admin@fashion.com", "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String refreshToken = provider.generateRefreshToken(auth);
        String accessToken = provider.generateAccessToken(auth);

        assertThat(provider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(provider.validateRefreshToken(accessToken)).isFalse();
        // Existing access-only validation is untouched: refresh tokens never pass it.
        assertThat(provider.validateToken(accessToken)).isTrue();
        assertThat(provider.validateToken(refreshToken)).isFalse();
    }

    @Test
    void validateRefreshTokenRejectsGarbage() {
        JwtTokenProvider provider = providerWithSecret(VALID_SECRET);

        assertThat(provider.validateRefreshToken("not-a-jwt")).isFalse();
        assertThat(provider.validateRefreshToken("")).isFalse();
    }

    @Test
    void exposesConfiguredAccessTokenLifetime() {
        JwtTokenProvider provider = providerWithSecret(VALID_SECRET);

        assertThat(provider.getAccessTokenExpirationMs()).isEqualTo(900_000L);
    }

    @Test
    void rejectsShortSecretAtStartup() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "too-short-secret"); // < 64 bytes

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("64 bytes");
    }

    @Test
    void rejectsMissingSecretAtStartup() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "");

        assertThatThrownBy(provider::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }
}
