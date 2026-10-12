package com.fashion.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin collection CRUD (slice D): full round trip against the seeded seasons
 * (PV2026/OI2026), X-Total-Count, slug duplicate → 409, unknown season → 400,
 * and soft delete verified by a 404 on the detail. Also covers the read-only
 * seasons reference endpoint used by the collection editor.
 */
@AutoConfigureMockMvc
class AdminCollectionIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@fashion.com";
    private static final String ADMIN_PASSWORD = "Admin@2026!";

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
    void seedAdmin() {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", ADMIN_EMAIL);
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = 'ADMIN'", Long.class);
        jdbcTemplate.update("""
                        INSERT INTO users (uuid, email, password_hash, first_name, last_name,
                                           is_active, is_verified, role_id, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, true, true, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""",
                UUID.randomUUID(), ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), "Admin", "Maison", roleId);
    }

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/collections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCollectionRoundTripWithSeasonAndXTotalCount() throws Exception {
        String slug = uniqueSlug("capsula-verano");
        String body = createCollection("Cápsula Verano", slug, 1L);

        JsonNode created = objectMapper.readTree(body);
        assertThat(created.get("slug").asText()).isEqualTo(slug);
        assertThat(created.get("seasonId").asLong()).isEqualTo(1L);
        assertThat(created.get("seasonName").asText()).isNotBlank();
        assertThat(created.get("isFeatured").asBoolean()).isFalse();

        MvcResult list = mockMvc.perform(get("/api/v1/admin/collections")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andReturn();

        JsonNode page = objectMapper.readTree(list.getResponse().getContentAsString());
        assertThat(list.getResponse().getHeader("X-Total-Count"))
                .isEqualTo(String.valueOf(page.get("totalElements").asLong()));
        assertThat(page.get("content")).anyMatch(n -> n.get("slug").asText().equals(slug));
    }

    @Test
    void createWithDuplicateSlugReturns409Conflict() throws Exception {
        String slug = uniqueSlug("capsula-duplicada");
        createCollection("Cápsula duplicada", slug, 1L);

        mockMvc.perform(post("/api/v1/admin/collections")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(collectionBody("Cápsula duplicada 2", slug, 1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void updateCollectionModifiesFieldsAndSlugConflictIs409() throws Exception {
        String slugA = uniqueSlug("capsula-a");
        long id = idOf(createCollection("Cápsula A", slugA, 1L));

        mockMvc.perform(put("/api/v1/admin/collections/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(collectionBody("Cápsula A editada", slugA, 2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cápsula A editada"))
                .andExpect(jsonPath("$.seasonId").value(2L));

        String slugB = uniqueSlug("capsula-b");
        createCollection("Cápsula B", slugB, 1L);
        mockMvc.perform(put("/api/v1/admin/collections/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(collectionBody("Cápsula A editada", slugB, 1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createWithUnknownSeasonReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/collections")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(collectionBody("Sin temporada", uniqueSlug("sin-temporada"), 999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void deleteCollectionSoftDeletesItAndDetailBecomes404() throws Exception {
        String slug = uniqueSlug("capsula-efimera");
        long id = idOf(createCollection("Cápsula efímera", slug, 1L));

        mockMvc.perform(delete("/api/v1/admin/collections/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/collections/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void notFoundCollectionReturns404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/collections/999999999")
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void createWithInvalidPayloadReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/collections")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"sin-nombre\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").exists());
    }

    @Test
    void seasonsEndpointListsSeededSeasons() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/seasons")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode seasons = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(seasons).anyMatch(n -> n.get("code").asText().equals("PV2026"));
        assertThat(seasons).anyMatch(n -> n.get("code").asText().equals("OI2026"));
    }

    // ---------- helpers ----------

    private String bearer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String collectionBody(String name, String slug, Long seasonId) {
        return "{\"name\":\"" + name + "\",\"slug\":\"" + slug
                + "\",\"seasonId\":" + seasonId + "}";
    }

    private String createCollection(String name, String slug, Long seasonId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/collections")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(collectionBody(name, slug, seasonId)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private long idOf(String detailJson) throws Exception {
        return objectMapper.readTree(detailJson).get("id").asLong();
    }
}
