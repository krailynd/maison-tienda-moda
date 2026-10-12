package com.fashion.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin product images (slice C): multipart upload with content-type
 * whitelist + size guard, primary demotion, delete with physical file removal,
 * and the /uploads URL contract.
 */
@AutoConfigureMockMvc
class AdminImageIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@fashion.com";
    private static final String ADMIN_PASSWORD = "Admin@2026!";
    private static final String UPLOADS_TEST_DIR = tempUploadsDir();

    private static String tempUploadsDir() {
        try {
            return Files.createTempDirectory("admin-images-test").toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create temp uploads dir", e);
        }
    }

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

    @DynamicPropertySource
    static void uploadsDir(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.dir", () -> UPLOADS_TEST_DIR);
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
    void uploadImageReturns201WithUploadsUrlAndBecomesPrimary() throws Exception {
        long productId = createProduct();
        MockMultipartFile file = new MockMultipartFile("file", "frente.png", "image/png", new byte[]{1, 2, 3, 4});

        MvcResult result = mockMvc.perform(multipart("/api/v1/admin/products/" + productId + "/images")
                        .file(file)
                        .header("Authorization", bearer())
                        .param("altText", "Frente"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/uploads/")))
                .andExpect(jsonPath("$.thumbnailUrl").doesNotExist())
                .andExpect(jsonPath("$.altText").value("Frente"))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andReturn();

        JsonNode image = objectMapper.readTree(result.getResponse().getContentAsString());
        String filename = image.get("imageUrl").asText().substring("/uploads/".length());
        // The file actually landed on disk.
        assertThat(Files.exists(Path.of(UPLOADS_TEST_DIR, filename))).isTrue();
    }

    @Test
    void uploadRejectsUnsupportedContentTypeWith400ValidationError() throws Exception {
        long productId = createProduct();
        MockMultipartFile file = new MockMultipartFile("file", "malware.svg", "image/svg+xml", "<svg/>".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/" + productId + "/images")
                        .file(file)
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.file").exists());

        assertProductHasNoImages(productId);
    }

    @Test
    void uploadRejectsOversizeFileWith400() throws Exception {
        long productId = createProduct();
        MockMultipartFile file = new MockMultipartFile("file", "huge.png", "image/png", new byte[10 * 1024 * 1024 + 1]);

        mockMvc.perform(multipart("/api/v1/admin/products/" + productId + "/images")
                        .file(file)
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        assertProductHasNoImages(productId);
    }

    @Test
    void uploadForMissingProductReturns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "frente.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart("/api/v1/admin/products/999999999/images")
                        .file(file)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void setPrimaryDemotesThePreviousPrimary() throws Exception {
        long productId = createProduct();
        long first = upload(productId, "first.png", "image/png", "Primera");
        long second = upload(productId, "second.png", "image/png", "Segunda");

        assertThat(primaryId(productId)).isEqualTo(first);

        mockMvc.perform(post("/api/v1/admin/images/" + second + "/primary")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second))
                .andExpect(jsonPath("$.isPrimary").value(true));

        assertThat(primaryId(productId)).isEqualTo(second);
    }

    @Test
    void deleteImageRemovesFileAndSoftDeletesRow() throws Exception {
        long productId = createProduct();
        long first = upload(productId, "one.png", "image/png", "Uno");
        long second = upload(productId, "two.png", "image/png", "Dos");
        String filename = imageFilename(productId, first);

        mockMvc.perform(delete("/api/v1/admin/images/" + first)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        // Physical file gone from disk.
        assertThat(Files.exists(Path.of(UPLOADS_TEST_DIR, filename))).isFalse();
        // Row gone from the admin detail (soft-delete via @Where).
        JsonNode detail = adminDetail(productId);
        assertThat(detail.get("images")).noneMatch(node -> node.get("id").asLong() == first);
        assertThat(detail.get("images")).anyMatch(node -> node.get("id").asLong() == second);
        // The remaining image was promoted to primary (no broken hero URL).
        assertThat(primaryId(productId)).isEqualTo(second);
    }

    @Test
    void setPrimaryForMissingImageReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/admin/images/999999999/primary")
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
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

    private long createProduct() throws Exception {
        String slug = "producto-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Producto con fotos\",\"slug\":\"" + slug + "\",\"price\":199.00}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long upload(long productId, String filename, String contentType, String altText) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, new byte[]{9, 8, 7});
        MvcResult result = mockMvc.perform(multipart("/api/v1/admin/products/" + productId + "/images")
                        .file(file)
                        .header("Authorization", bearer())
                        .param("altText", altText))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode adminDetail(long productId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/products/" + productId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long primaryId(long productId) throws Exception {
        JsonNode images = adminDetail(productId).get("images");
        for (JsonNode image : images) {
            if (image.get("primary").asBoolean()) {
                return image.get("id").asLong();
            }
        }
        return -1;
    }

    private String imageFilename(long productId, long imageId) throws Exception {
        JsonNode detail = adminDetail(productId);
        for (JsonNode image : detail.get("images")) {
            if (image.get("id").asLong() == imageId) {
                return image.get("url").asText().substring("/uploads/".length());
            }
        }
        throw new IllegalStateException("Image not found in detail: " + imageId);
    }

    private void assertProductHasNoImages(long productId) throws Exception {
        assertThat(adminDetail(productId).get("images")).isEmpty();
    }
}
