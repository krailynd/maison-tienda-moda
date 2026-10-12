package com.fashion.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Customer registration flow: 201 with an auto-login token pair, duplicate
 * email → 409, bean validation → 400, and proof that the issued CUSTOMER token
 * cannot reach admin endpoints. Uses a real Redis Testcontainer because the
 * auto-login refresh flow stores the refresh token server-side.
 */
@AutoConfigureMockMvc
class AuthRegisterIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_EMAIL = "cliente@fashion.com";
    private static final String TEST_PASSWORD = "Cliente2026!";
    private static final String FIRST_NAME = "María";
    private static final String LAST_NAME = "Gómez";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "redis123");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "redis123");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupUsers() {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", TEST_EMAIL);
    }

    @Test
    void registerWithValidPayloadReturns201AndTokenPair() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(TEST_EMAIL, TEST_PASSWORD, FIRST_NAME, LAST_NAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.firstName").value(FIRST_NAME))
                .andExpect(jsonPath("$.user.lastName").value(LAST_NAME))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));

        // The auto-login session means the new credentials must log in.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = 'CUSTOMER'", Long.class);
        jdbcTemplate.update("""
                        INSERT INTO users (uuid, email, password_hash, first_name, last_name,
                                           is_active, is_verified, role_id, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, true, true, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""",
                UUID.randomUUID(), TEST_EMAIL, passwordEncoder.encode("ExistingPass1!"), "Ya", "Existe", roleId);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(TEST_EMAIL, TEST_PASSWORD, FIRST_NAME, LAST_NAME)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void registerWithInvalidPayloadReturns400() throws Exception {
        // Missing firstName and too-short password.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"otra@fashion.com\",\"password\":\"short\",\"lastName\":\"Gómez\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // Missing lastName and invalid email.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-correo\",\"password\":\"ValidPass1!\",\"firstName\":\"María\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void customerCannotAccessAdminEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(TEST_EMAIL, TEST_PASSWORD, FIRST_NAME, LAST_NAME)))
                .andExpect(status().isCreated())
                .andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    private String registerBody(String email, String password, String firstName, String lastName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}";
    }

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
