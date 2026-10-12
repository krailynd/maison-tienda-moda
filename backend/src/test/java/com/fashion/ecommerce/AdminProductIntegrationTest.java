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
 * Admin product catalog (slice B): CRUD, admin-only filters (drafts + inactive
 * included), X-Total-Count, draft-by-default on create, slug conflict → 409,
 * publish/unpublish toggles and the CAT-6 gap (draft products must be hidden
 * from the storefront detail but visible in the admin detail).
 */
@AutoConfigureMockMvc
class AdminProductIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProductDefaultsToDraftAndAppearsInAdminList() throws Exception {
        String slug = uniqueSlug("blusa");
        String body = createProduct(slug, "Blusa de prueba", "199.00", 1L, null);

        assertThat(objectMapper.readTree(body).get("isActive").asBoolean()).isFalse();
        assertThat(objectMapper.readTree(body).get("slug").asText()).isEqualTo(slug);

        MvcResult list = mockMvc.perform(get("/api/v1/admin/products")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andReturn();

        JsonNode page = objectMapper.readTree(list.getResponse().getContentAsString());
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(list.getResponse().getHeader("X-Total-Count"))
                .isEqualTo(String.valueOf(page.get("totalElements").asLong()));
        assertThat(page.get("content")).anyMatch(node -> node.get("slug").asText().equals(slug));
    }

    @Test
    void createWithDuplicateSlugReturns409Conflict() throws Exception {
        String slug = uniqueSlug("abrigo");
        createProduct(slug, "Abrigo uno", "500.00", null, null);

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("Abrigo duplicado", slug, "600.00", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void updateProductModifiesFieldsAndSlugConflictIs409() throws Exception {
        String slugA = uniqueSlug("pantalon-a");
        String slugB = uniqueSlug("pantalon-b");
        long id = idOf(createProduct(slugA, "Pantalón A", "250.00", null, null));

        // Normal update.
        mockMvc.perform(put("/api/v1/admin/products/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("Pantalón A editado", slugA, "270.00", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pantalón A editado"))
                .andExpect(jsonPath("$.price").value(270.00))
                .andExpect(jsonPath("$.isActive").value(false));

        // Renaming to a slug used by another product → 409 CONFLICT.
        createProduct(slugB, "Pantalón B", "280.00", null, null);
        mockMvc.perform(put("/api/v1/admin/products/" + id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("Pantalón A editado", slugB, "270.00", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void adminListFiltersByStatusSearchCategoryAndBrand() throws Exception {
        String alphaSlug = uniqueSlug("alpha");
        createProduct(alphaSlug, "Alpha de lino", "300.00", 1L, maisonBrandId());
        String betaSlug = uniqueSlug("beta");
        long betaId = idOf(createProduct(betaSlug, "Beta de algodón", "150.00", 2L, null));
        publish(betaId);

        // status=active only returns the published product.
        MvcResult active = mockMvc.perform(get("/api/v1/admin/products").param("status", "active")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(active)).noneMatch(n -> n.get("slug").asText().equals(alphaSlug));
        assertThat(contentOf(active)).anyMatch(n -> n.get("slug").asText().equals(betaSlug));

        // status=inactive only returns the draft.
        MvcResult inactive = mockMvc.perform(get("/api/v1/admin/products").param("status", "inactive")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(inactive)).anyMatch(n -> n.get("slug").asText().equals(alphaSlug));
        assertThat(contentOf(inactive)).noneMatch(n -> n.get("slug").asText().equals(betaSlug));

        // search filters by name.
        MvcResult search = mockMvc.perform(get("/api/v1/admin/products").param("search", "lino")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(search)).anyMatch(n -> n.get("slug").asText().equals(alphaSlug));

        // categoryId filter.
        MvcResult byCategory = mockMvc.perform(get("/api/v1/admin/products").param("categoryId", "1")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(byCategory)).anyMatch(n -> n.get("slug").asText().equals(alphaSlug));

        // brandId filter.
        MvcResult byBrand = mockMvc.perform(get("/api/v1/admin/products").param("brandId", String.valueOf(maisonBrandId()))
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(byBrand)).anyMatch(n -> n.get("slug").asText().equals(alphaSlug));
    }

    @Test
    void deleteProductSoftDeletesItFromAdminAndStorefront() throws Exception {
        String slug = uniqueSlug("cartera");
        long id = idOf(createProduct(slug, "Cartera de cuero", "999.00", null, null));

        mockMvc.perform(delete("/api/v1/admin/products/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        // Admin detail now 404 (soft-deleted row is filtered by @Where).
        mockMvc.perform(get("/api/v1/admin/products/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        // Storefront detail 404 as well.
        mockMvc.perform(get("/api/v1/products/" + slug))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishAndUnpublishToggleStorefrontVisibility() throws Exception {
        String slug = uniqueSlug("vestido");
        long id = idOf(createProduct(slug, "Vestido de noche", "450.00", null, null));

        // Draft: storefront detail 404.
        mockMvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isNotFound());

        // Publish → storefront detail 200, admin detail isActive=true.
        mockMvc.perform(post("/api/v1/admin/products/" + id + "/publish")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
        mockMvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isOk());

        // Unpublish → storefront detail back to 404.
        mockMvc.perform(post("/api/v1/admin/products/" + id + "/unpublish")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
        mockMvc.perform(get("/api/v1/products/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void cat6DraftIsHiddenFromStorefrontButVisibleInAdminDetail() throws Exception {
        String slug = uniqueSlug("borrador");
        long id = idOf(createProduct(slug, "Producto borrador", "100.00", null, null));

        // CAT-6: the storefront must never serve a draft (404, not a leak).
        mockMvc.perform(get("/api/v1/products/" + slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        // The storefront list excludes it too.
        MvcResult storefrontList = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(storefrontList)).noneMatch(n -> n.get("slug").asText().equals(slug));

        // The admin detail still sees the draft.
        mockMvc.perform(get("/api/v1/admin/products/" + id)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void notFoundAdminProductReturns404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/999999999")
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void createWithInvalidPayloadReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"sin-nombre\",\"price\":-5}"))
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

    private String productBody(String name, String slug, String price, Long categoryId, Long brandId) {
        StringBuilder json = new StringBuilder()
                .append("{\"name\":\"").append(name)
                .append("\",\"slug\":\"").append(slug)
                .append("\",\"price\":").append(price);
        if (categoryId != null) {
            json.append(",\"categoryId\":").append(categoryId);
        }
        if (brandId != null) {
            json.append(",\"brandId\":").append(brandId);
        }
        return json.append("}").toString();
    }

    private String createProduct(String slug, String name, String price, Long categoryId, Long brandId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(name, slug, price, categoryId, brandId)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void publish(long id) throws Exception {
        mockMvc.perform(post("/api/v1/admin/products/" + id + "/publish")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    private long idOf(String detailJson) throws Exception {
        return objectMapper.readTree(detailJson).get("id").asLong();
    }

    private JsonNode contentOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
    }

    private Long maisonBrandId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM brands WHERE slug = 'maison' AND deleted_at IS NULL", Long.class);
    }
}
