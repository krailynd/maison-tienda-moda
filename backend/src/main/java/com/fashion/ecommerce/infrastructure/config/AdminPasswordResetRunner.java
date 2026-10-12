package com.fashion.ecommerce.infrastructure.config;

import com.fashion.ecommerce.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Dev bootstrap: when {@code ADMIN_DEFAULT_PASSWORD} ({@code app.admin.default-password})
 * is set, encodes it with the real BCrypt(12) encoder and writes
 * {@code users.password_hash} for {@code admin@fashion.com} through the JPA
 * repository (no raw SQL, no password literals in SQL). When the env var is
 * absent the runner is a no-op.
 * <p>
 * The update is skipped when the stored hash already matches the default
 * password, so every application restart does not churn the hash.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminPasswordResetRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-password:}")
    private String adminDefaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminDefaultPassword)) {
            log.info("ADMIN_DEFAULT_PASSWORD not set — skipping admin password reset");
            return;
        }

        userRepository.findByEmail("admin@fashion.com").ifPresent(user -> {
            if (passwordEncoder.matches(adminDefaultPassword, user.getPasswordHash())) {
                log.debug("Admin password already matches default — no update needed");
                return;
            }
            user.setPasswordHash(passwordEncoder.encode(adminDefaultPassword));
            userRepository.save(user);
            log.info("Admin default password applied for admin@fashion.com");
        });
    }
}
