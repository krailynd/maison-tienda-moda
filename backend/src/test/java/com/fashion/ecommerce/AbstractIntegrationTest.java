package com.fashion.ecommerce;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests (design D9).
 * <p>
 * Boots the full Spring context against a real PostgreSQL 16 container;
 * Flyway applies {@code V1__initial_schema.sql} as part of the context
 * startup. Redis / RabbitMQ are intentionally not required: their clients are
 * lazy and only fail at use time.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fashion_ecommerce")
            .withUsername("postgres")
            .withPassword("postgres123");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
