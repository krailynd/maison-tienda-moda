package com.fashion.ecommerce.infrastructure.config;

import com.fashion.ecommerce.domain.model.User;
import com.fashion.ecommerce.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminPasswordResetRunner}: no-op without
 * {@code ADMIN_DEFAULT_PASSWORD}, BCrypt encode + JPA update when set, and no
 * update when the stored hash already matches.
 */
class AdminPasswordResetRunnerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminPasswordResetRunner runner =
            new AdminPasswordResetRunner(userRepository, passwordEncoder);

    @Test
    void noOpWhenEnvNotSet() {
        ReflectionTestUtils.setField(runner, "adminDefaultPassword", "");

        runner.run(null);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void encodesAndUpdatesAdminPasswordWhenEnvSet() {
        ReflectionTestUtils.setField(runner, "adminDefaultPassword", "Admin@2026!");
        User admin = new User();
        admin.setPasswordHash("$2a$12$old-hash");
        when(userRepository.findByEmail("admin@fashion.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Admin@2026!", "$2a$12$old-hash")).thenReturn(false);
        when(passwordEncoder.encode("Admin@2026!")).thenReturn("$2a$12$new-hash");

        runner.run(null);

        verify(passwordEncoder).encode("Admin@2026!");
        assertThat(admin.getPasswordHash()).isEqualTo("$2a$12$new-hash");
        verify(userRepository).save(admin);
    }

    @Test
    void skipsUpdateWhenHashAlreadyMatches() {
        ReflectionTestUtils.setField(runner, "adminDefaultPassword", "Admin@2026!");
        User admin = new User();
        admin.setPasswordHash("$2a$12$matching-hash");
        when(userRepository.findByEmail("admin@fashion.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Admin@2026!", "$2a$12$matching-hash")).thenReturn(true);

        runner.run(null);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void ignoresMissingAdminUser() {
        ReflectionTestUtils.setField(runner, "adminDefaultPassword", "Admin@2026!");
        when(userRepository.findByEmail("admin@fashion.com")).thenReturn(Optional.empty());

        runner.run(null);

        verify(userRepository).findByEmail("admin@fashion.com");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
