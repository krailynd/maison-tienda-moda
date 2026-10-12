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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin orders (slice E): full status walk PENDING→…→DELIVERED, illegal
 * transition → 409 with the status unchanged, unknown status → 400, missing
 * order → 404, guest-order null-safety (user null → email/name from the JSONB
 * snapshot), CANCELLED/REFUNDED side effects, and the list filters backed by
 * the X-Total-Count header.
 */
@AutoConfigureMockMvc
class AdminOrderIntegrationTest extends AbstractIntegrationTest {

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
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/orders/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullStatusWalkPendingToDeliveredRecordsHistoryAndShippingStamps() throws Exception {
        long orderId = createGuestOrder("walk-" + unique() + "@example.com");

        transition(orderId, statusBody("CONFIRMED")).andExpect(status().isOk());
        transition(orderId, statusBody("PROCESSING")).andExpect(status().isOk());
        transition(orderId, statusBody("SHIPPED")).andExpect(status().isOk());
        transition(orderId, statusBody("DELIVERED")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.shippedAt").exists())
                .andExpect(jsonPath("$.deliveredAt").exists())
                .andExpect(jsonPath("$.statusHistory.length()").value(4))
                .andExpect(jsonPath("$.statusHistory[0].oldStatus").value("PENDING"))
                .andExpect(jsonPath("$.statusHistory[0].newStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.statusHistory[1].newStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.statusHistory[2].newStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.statusHistory[3].newStatus").value("DELIVERED"));
    }

    @Test
    void illegalTransitionReturns409AndLeavesStatusUnchanged() throws Exception {
        long orderId = createGuestOrder("illegal-" + unique() + "@example.com");

        // PENDING → SHIPPED skips two states: rejected, and the order must not
        // change at all (no status history entry for the failed attempt).
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/status")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("SHIPPED")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.statusHistory.length()").value(0));
    }

    @Test
    void unknownStatusReturns400() throws Exception {
        long orderId = createGuestOrder("unknown-" + unique() + "@example.com");

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/status")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void unknownOrderIdReturns404ForDetailAndTransition() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/999999999")
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        mockMvc.perform(post("/api/v1/admin/orders/999999999/status")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("CONFIRMED")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void guestOrderDetailIsNullSafe() throws Exception {
        String email = "guest-" + unique() + "@example.com";
        long orderId = createGuestOrder(email);

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userEmail").value(nullValue()))
                .andExpect(jsonPath("$.customerName").value("Guest Buyer"))
                .andExpect(jsonPath("$.customer.email").value(email))
                .andExpect(jsonPath("$.shippingAddress.email").value(email))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value(
                        org.hamcrest.Matchers.containsString("Blusa de Seda Marfil")))
                .andExpect(jsonPath("$.payments.length()").value(0))
                .andExpect(jsonPath("$.statusHistory.length()").value(0));
    }

    @Test
    void cancelAndRefundSideEffectsArePersisted() throws Exception {
        long orderId = createGuestOrder("refund-" + unique() + "@example.com");

        transition(orderId, "{\"newStatus\":\"CANCELLED\",\"notes\":\"Customer changed mind\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists())
                .andExpect(jsonPath("$.cancellationReason").value("Customer changed mind"))
                .andExpect(jsonPath("$.allowedTransitions").value(org.hamcrest.Matchers.contains("REFUNDED")));

        transition(orderId, "{\"newStatus\":\"REFUNDED\",\"refundAmount\":289.00}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundAmount").value(289.00))
                .andExpect(jsonPath("$.allowedTransitions.length()").value(0));
    }

    @Test
    void listFiltersByStatusPaymentStatusAndEmailWithTotalCount() throws Exception {
        String pendingEmail = "pending-" + unique() + "@example.com";
        long pendingId = createGuestOrder(pendingEmail);

        String shippedEmail = "shipped-" + unique() + "@example.com";
        long shippedId = createGuestOrder(shippedEmail);
        transition(shippedId, statusBody("CONFIRMED")).andExpect(status().isOk());
        transition(shippedId, statusBody("PROCESSING")).andExpect(status().isOk());
        transition(shippedId, statusBody("SHIPPED")).andExpect(status().isOk());

        // Payment status: pay the shipped order through the public endpoint.
        String shippedNumber = orderNumber(shippedId);
        mockMvc.perform(post("/api/v1/checkout/orders/" + shippedNumber + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"card\"}"))
                .andExpect(status().isOk());

        // status filter → every row matches.
        MvcResult byStatus = mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "PENDING")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andReturn();
        assertThat(contentOf(byStatus)).allMatch(n -> n.get("status").asText().equals("PENDING"));
        assertTotalCountMatches(byStatus);

        // paymentStatus filter → the paid order is included.
        MvcResult byPayment = mockMvc.perform(get("/api/v1/admin/orders")
                        .param("paymentStatus", "PAID")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(byPayment)).anyMatch(n -> n.get("id").asLong() == shippedId);
        assertTotalCountMatches(byPayment);

        // email search → guest order matched through its JSONB snapshot.
        MvcResult byEmail = mockMvc.perform(get("/api/v1/admin/orders")
                        .param("search", pendingEmail)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(byEmail)).anyMatch(n -> n.get("id").asLong() == pendingId);
        assertTotalCountMatches(byEmail);

        // orderNumber search works too.
        MvcResult byNumber = mockMvc.perform(get("/api/v1/admin/orders")
                        .param("search", orderNumber(shippedId))
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(contentOf(byNumber)).anyMatch(n -> n.get("id").asLong() == shippedId);
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

    /**
     * Creates a guest order through the public checkout endpoint (seeded
     * product 1 with its first active variant — V2 rewrites the V1 catalog, so
     * variant ids are resolved from the DB, never hard-coded) and returns its
     * id.
     */
    private long createGuestOrder(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/checkout/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String orderNumber(long orderId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("orderNumber").asText();
    }

    private org.springframework.test.web.servlet.ResultActions transition(long orderId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/status")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String statusBody(String newStatus) {
        return "{\"newStatus\":\"" + newStatus + "\"}";
    }

    private String orderBody(String email) {
        long variantId = jdbcTemplate.queryForObject(
                "SELECT id FROM product_variants WHERE product_id = 1 AND is_active = true ORDER BY id LIMIT 1",
                Long.class);
        return "{\"items\":[{\"productId\":1,\"variantId\":" + variantId + ",\"quantity\":1}],"
                + "\"customer\":{"
                + "\"firstName\":\"Guest\",\"lastName\":\"Buyer\",\"email\":\"" + email + "\","
                + "\"phone\":\"555-0100\",\"addressLine1\":\"Av. Test 123\","
                + "\"city\":\"Lima\",\"region\":\"Lima\",\"postalCode\":\"15001\",\"countryCode\":\"PE\"},"
                + "\"notes\":\"admin-orders test\"}";
    }

    private void assertTotalCountMatches(MvcResult result) throws Exception {
        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getHeader("X-Total-Count"))
                .isEqualTo(String.valueOf(page.get("totalElements").asLong()));
    }

    private JsonNode contentOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
    }

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
