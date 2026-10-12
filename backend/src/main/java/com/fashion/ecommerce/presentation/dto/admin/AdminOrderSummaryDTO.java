package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin order list row for {@code GET /api/v1/admin/orders}.
 * <p>
 * Guest orders (no registered {@code user_id}) are included: their email and
 * name are resolved null-safe from the JSONB address snapshot.
 */
public record AdminOrderSummaryDTO(
        Long id,
        String orderNumber,
        String status,
        String paymentStatus,
        String customerEmail,
        String customerName,
        BigDecimal totalAmount,
        String currencyCode,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminOrderSummaryDTO from(Order order, int itemCount, ObjectMapper objectMapper) {
        Map<String, Object> address = readJson(order.getBillingAddress(), objectMapper);
        String userEmail = order.getUser() != null ? order.getUser().getEmail() : null;
        String email = userEmail != null ? userEmail : str(address, "email");
        String name = order.getUser() != null
                ? joinName(order.getUser().getFirstName(), order.getUser().getLastName())
                : joinName(str(address, "firstName"), str(address, "lastName"));
        return new AdminOrderSummaryDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                email,
                name,
                order.getTotalAmount(),
                order.getCurrencyCode(),
                itemCount,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static Map<String, Object> readJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
            return map != null ? map : Map.of();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private static String str(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private static String joinName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) {
            sb.append(first.trim());
        }
        if (last != null && !last.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(last.trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
