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
 * Admin category CRUD (slice D): full round trip, inactive categories visible
 * to the admin list, X-Total-Count, slug duplicate → 409, and the delete guard
 * (categories with children or products → 409). Soft delete verified by a 404
 * on the detail after DELETE.
 */
@AutoConfigureMockMvc
class AdminCategoryIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategoryRoundTripWithInactiveVisibleInAdminList() throws Exception {
        String slug = uniqueSlug("temporada");
        String body = createCategory("Temporada de prueba", slug, null, null);

        JsonNode created = objectMapper.readTree(body);
        assertThat(created.get("slug").asText()).isEqualTo(slug);
        assertThat(created.get("isActive").asBoolean()).isTrue();

        // An explicitly inactive category is still visible to the admin list.
        String inactiveSlug = uniqueSlug("oculta");
        createCategory("Categoría oculta", inactiveSlug, false, null);

        MvcResult list = mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andReturn();

        JsonNode page = objectMapper.readTree(list.getResponse().getContentAsString());
        assertThat(list.getResponse().getHeader("X-Total-Count"))
                .isEqualTo(String.valueOf(page.get("totalElements").asLong()));
        assertThat(page.get("content")).anyMatch(n -> n.get("slug").asText().equals(slug));
        assertThat(page.get("content")).anyMatch(n -> n.get("slug").asText().equals(inactiveSlug));
    }

    @Test
    void createWithDuplicateSlugReturns409Conflict() throws Exception {
        String slug = uniqueSlug("ropa");
        createCategory("Ropa", slug, null, null);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("Ropa duplicada", slug, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void updateCategoryModifiesFieldsAndSlugConflictIs409() throws Exception {
        String slugA = uniqueSlug("calzado-a");
        long id = idOf(createCategory("Calzado A", slugA, null, null));

        mockMvc.perform(put("/api/v1/admin/categories/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("Calzado A editado", slugA, null, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Calzado A editado"))
                .andExpect(jsonPath("$.displayOrder").value(5));

        String slugB = uniqueSlug("calzado-b");
        createCategory("Calzado B", slugB, null, null);
        mockMvc.perform(put("/api/v1/admin/categories/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("Calzado A editado", slugB, null, 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void deleteCategoryWithProductsReturns409Conflict() throws Exception {
        String slug = uniqueSlug("accesorios-nuevos");
        long categoryId = idOf(createCategory("Accesorios nuevos", slug, null, null));

        // A product referencing the category blocks its deletion.
        String productSlug = uniqueSlug("producto-atado");
        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("Producto atado", productSlug, categoryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/admin/categories/" + categoryId)
                        .header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void deleteCategoryWithChildrenReturns409Conflict() throws Exception {
        String parentSlug = uniqueSlug("padre");
        long parentId = idOf(createCategory("Padre", parentSlug, null, null));
        String childSlug = uniqueSlug("hijo");
        createCategory("Hijo", childSlug, null, parentId);

        mockMvc.perform(delete("/api/v1/admin/categories/" + parentId)
                        .header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void deleteCategorySoftDeletesItAndDetailBecomes404() throws Exception {
        String slug = uniqueSlug("efimera");
        long id = idOf(createCategory("Efímera", slug, null, null));

        mockMvc.perform(delete("/api/v1/admin/categories/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/categories/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void notFoundCategoryReturns404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories/999999999")
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void createWithInvalidPayloadReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"solo-slug\"}"))
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

    private String categoryBody(String name, String slug, Boolean isActive, Integer displayOrder) {
        StringBuilder json = new StringBuilder()
                .append("{\"name\":\"").append(name)
                .append("\",\"slug\":\"").append(slug).append("\"");
        if (isActive != null) {
            json.append(",\"isActive\":").append(isActive);
        }
        if (displayOrder != null) {
            json.append(",\"displayOrder\":").append(displayOrder);
        }
        return json.append("}").toString();
    }

    private String createCategory(String name, String slug, Boolean isActive, Long parentId) throws Exception {
        StringBuilder json = new StringBuilder(categoryBody(name, slug, isActive, null));
        if (parentId != null) {
            json.insert(json.length() - 1, ",\"parentId\":" + parentId);
        }
        MvcResult result = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String productBody(String name, String slug, Long categoryId) {
        return "{\"name\":\"" + name + "\",\"slug\":\"" + slug
                + "\",\"price\":100.00,\"categoryId\":" + categoryId + "}";
    }

    private long idOf(String detailJson) throws Exception {
        return objectMapper.readTree(detailJson).get("id").asLong();
    }
}
