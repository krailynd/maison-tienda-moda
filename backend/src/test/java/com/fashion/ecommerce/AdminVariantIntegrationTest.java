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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin product variants (slice C): create under a product, SKU uniqueness →
 * 409, product/variant 404s, update semantics, soft delete and the stock
 * mapping to the variant's default-warehouse inventory row.
 */
@AutoConfigureMockMvc
class AdminVariantIntegrationTest extends AbstractIntegrationTest {

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
    void createVariantReturns201WithStockAndReferences() throws Exception {
        long productId = createProduct();

        MvcResult result = mockMvc.perform(post("/api/v1/admin/products/" + productId + "/variants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Blusa - M / Negro", uniqueSku("TST"), "3", "1", "199.00", "12", "true")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Blusa - M / Negro"))
                .andExpect(jsonPath("$.stock").value(12))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.size.id").value(3))
                .andExpect(jsonPath("$.color.id").value(1))
                .andReturn();

        JsonNode variant = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(variant.get("sku").asText()).startsWith("TST-");
        assertThat(variant.get("comparePrice").isNull()).isTrue();
    }

    @Test
    void createWithDuplicateSkuReturns409Conflict() throws Exception {
        long productId = createProduct();
        String sku = uniqueSku("DUP");

        mockMvc.perform(post("/api/v1/admin/products/" + productId + "/variants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Variant one", sku, null, null, "100.00", "5", "false")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/products/" + productId + "/variants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Variant two", sku, null, null, "110.00", "5", "false")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createVariantForMissingProductReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products/999999999/variants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Variant", uniqueSku("NOPE"), null, null, "100.00", "1", "false")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updateVariantModifiesFieldsAndStock() throws Exception {
        long productId = createProduct();
        long variantId = createVariant(productId, uniqueSku("UPD"), "10");

        mockMvc.perform(put("/api/v1/admin/variants/" + variantId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Renamed variant", uniqueSku("UPD2"), "2", null, "150.00", "25", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed variant"))
                .andExpect(jsonPath("$.price").value(150.00))
                .andExpect(jsonPath("$.stock").value(25))
                .andExpect(jsonPath("$.size.id").value(2))
                .andExpect(jsonPath("$.color").doesNotExist());
    }

    @Test
    void updateVariantWithDuplicateSkuReturns409() throws Exception {
        long productId = createProduct();
        String skuA = uniqueSku("A");
        String skuB = uniqueSku("B");
        long variantA = createVariant(productId, skuA, "5");
        createVariant(productId, skuB, "7");

        // Renaming variant A to variant B's SKU → 409.
        mockMvc.perform(put("/api/v1/admin/variants/" + variantA)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Variant A", skuB, null, null, "100.00", "5", "false")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void updateMissingVariantReturns404() throws Exception {
        mockMvc.perform(put("/api/v1/admin/variants/999999999")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Ghost", uniqueSku("GHO"), null, null, "1.00", "1", "false")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void deleteVariantSoftDeletesItFromAdminDetail() throws Exception {
        long productId = createProduct();
        long variantId = createVariant(productId, uniqueSku("DEL"), "8");

        mockMvc.perform(delete("/api/v1/admin/variants/" + variantId)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        // The variant no longer shows up in the admin product detail.
        MvcResult detail = mockMvc.perform(get("/api/v1/admin/products/" + productId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode variants = objectMapper.readTree(detail.getResponse().getContentAsString()).get("variants");
        assertThat(variants).noneMatch(node -> node.get("id").asLong() == variantId);

        // Direct variant access is gone too (404 via the update path).
        mockMvc.perform(put("/api/v1/admin/variants/" + variantId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Ghost", uniqueSku("GHO2"), null, null, "1.00", "1", "false")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithInvalidPayloadReturnsValidationEnvelope() throws Exception {
        long productId = createProduct();
        mockMvc.perform(post("/api/v1/admin/products/" + productId + "/variants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"NO-NAME\"}"))
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

    private String uniqueSku(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private long createProduct() throws Exception {
        String slug = "producto-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Producto de prueba\",\"slug\":\"" + slug + "\",\"price\":199.00}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createVariant(long productId, String sku, String stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/products/" + productId + "/variants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantBody("Variant " + sku, sku, null, null, "100.00", stock, "false")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String variantBody(String name, String sku, String sizeId, String colorId,
                               String price, String stock, String isDefault) {
        StringBuilder json = new StringBuilder()
                .append("{\"name\":\"").append(name)
                .append("\",\"sku\":\"").append(sku)
                .append("\",\"price\":").append(price)
                .append(",\"stock\":").append(stock)
                .append(",\"isDefault\":").append(isDefault);
        if (sizeId != null) {
            json.append(",\"sizeId\":").append(sizeId);
        }
        if (colorId != null) {
            json.append(",\"colorId\":").append(colorId);
        }
        return json.append("}").toString();
    }
}
