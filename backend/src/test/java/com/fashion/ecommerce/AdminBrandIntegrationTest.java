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
 * Admin brand CRUD (slice D): full round trip, X-Total-Count, slug duplicate →
 * 409, and the delete guard (brands referenced by products → 409, same
 * contract as categories). Soft delete verified by a 404 on the detail.
 */
@AutoConfigureMockMvc
class AdminBrandIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc.perform(get("/api/v1/admin/brands"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBrandRoundTripWithXTotalCount() throws Exception {
        String slug = uniqueSlug("marca-nueva");
        String body = createBrand("Marca Nueva", slug, null, null);

        JsonNode created = objectMapper.readTree(body);
        assertThat(created.get("slug").asText()).isEqualTo(slug);
        assertThat(created.get("isActive").asBoolean()).isTrue();

        MvcResult list = mockMvc.perform(get("/api/v1/admin/brands")
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
        String slug = uniqueSlug("duplicada");
        createBrand("Marca duplicada", slug, null, null);

        mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("Marca duplicada 2", slug, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void updateBrandModifiesFieldsAndSlugConflictIs409() throws Exception {
        String slugA = uniqueSlug("etiqueta-a");
        long id = idOf(createBrand("Etiqueta A", slugA, null, null));

        mockMvc.perform(put("/api/v1/admin/brands/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("Etiqueta A editada", slugA, true, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Etiqueta A editada"))
                .andExpect(jsonPath("$.isFeatured").value(true))
                .andExpect(jsonPath("$.displayOrder").value(3));

        String slugB = uniqueSlug("etiqueta-b");
        createBrand("Etiqueta B", slugB, null, null);
        mockMvc.perform(put("/api/v1/admin/brands/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody("Etiqueta A editada", slugB, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void deleteBrandWithProductsReturns409Conflict() throws Exception {
        // The seeded "maison" brand is referenced by the seeded catalog → 409.
        Long maisonId = jdbcTemplate.queryForObject(
                "SELECT id FROM brands WHERE slug = 'maison' AND deleted_at IS NULL", Long.class);

        mockMvc.perform(delete("/api/v1/admin/brands/" + maisonId)
                        .header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void deleteBrandSoftDeletesItAndDetailBecomes404() throws Exception {
        String slug = uniqueSlug("marca-efimera");
        long id = idOf(createBrand("Marca efímera", slug, null, null));

        mockMvc.perform(delete("/api/v1/admin/brands/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/brands/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void notFoundBrandReturns404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/brands/999999999")
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void createWithInvalidPayloadReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"sin-nombre\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").exists());
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

    private String brandBody(String name, String slug, Boolean isFeatured, Integer displayOrder) {
        StringBuilder json = new StringBuilder()
                .append("{\"name\":\"").append(name)
                .append("\",\"slug\":\"").append(slug).append("\"");
        if (isFeatured != null) {
            json.append(",\"isFeatured\":").append(isFeatured);
        }
        if (displayOrder != null) {
            json.append(",\"displayOrder\":").append(displayOrder);
        }
        return json.append("}").toString();
    }

    private String createBrand(String name, String slug, Boolean isFeatured, Integer displayOrder) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandBody(name, slug, isFeatured, displayOrder)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private long idOf(String detailJson) throws Exception {
        return objectMapper.readTree(detailJson).get("id").asLong();
    }
}
