package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Order;
import com.fashion.ecommerce.domain.model.OrderStatusHistory;
import com.fashion.ecommerce.presentation.dto.CustomerResponse;
import com.fashion.ecommerce.presentation.dto.OrderPaymentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Admin order detail for {@code GET /api/v1/admin/orders/{id}}.
 * <p>
 * Null-safe for guest orders (no registered user): the customer and the JSONB
 * addresses are resolved from the stored snapshot and may be null. The status
 * history is served chronologically (oldest first) and
 * {@code allowedTransitions} carries the legal successors of the current
 * status so the UI can render the transition control.
 */
public record AdminOrderDetailDTO(
        Long id,
        String orderNumber,
        String status,
        String paymentStatus,
        String userEmail,
        String customerName,
        CustomerResponse customer,
        CustomerResponse billingAddress,
        CustomerResponse shippingAddress,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal shippingCost,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currencyCode,
        String shippingMethod,
        String trackingNumber,
        String carrier,
        String paymentMethod,
        String paymentIntentId,
        String customerNotes,
        String internalNotes,
        String couponCode,
        BigDecimal couponDiscount,
        BigDecimal refundAmount,
        String cancellationReason,
        LocalDateTime cancelledAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        List<AdminOrderItemDTO> items,
        List<OrderPaymentResponse> payments,
        List<AdminOrderStatusHistoryDTO> statusHistory,
        List<String> allowedTransitions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminOrderDetailDTO from(Order order, ObjectMapper objectMapper) {
        Map<String, Object> addressJson = readJson(order.getShippingAddress(), objectMapper);
        CustomerResponse customer = CustomerResponse.fromJson(addressJson);

        List<AdminOrderItemDTO> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(AdminOrderItemDTO::from).toList();

        List<OrderPaymentResponse> payments = order.getPayments() == null
                ? List.of()
                : order.getPayments().stream().map(OrderPaymentResponse::from).toList();

        List<AdminOrderStatusHistoryDTO> statusHistory = order.getStatusHistory() == null
                ? List.of()
                : order.getStatusHistory().stream()
                        .sorted(Comparator.comparing(OrderStatusHistory::getChangedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(AdminOrderStatusHistoryDTO::from)
                        .toList();

        List<String> allowedTransitions = Order.allowedTransitions(order.getStatus()).stream()
                .map(Enum::name)
                .sorted()
                .toList();

        String userEmail = order.getUser() != null ? order.getUser().getEmail() : null;
        String customerName = userEmail != null
                ? joinName(order.getUser().getFirstName(), order.getUser().getLastName())
                : joinName(str(addressJson, "firstName"), str(addressJson, "lastName"));

        return new AdminOrderDetailDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                userEmail,
                customerName,
                customer,
                CustomerResponse.fromJson(readJson(order.getBillingAddress(), objectMapper)),
                CustomerResponse.fromJson(addressJson),
                order.getSubTotal(),
                order.getDiscountAmount(),
                order.getShippingCost(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCurrencyCode(),
                order.getShippingMethod(),
                order.getTrackingNumber(),
                order.getCarrier(),
                order.getPaymentMethod(),
                order.getPaymentIntentId(),
                order.getCustomerNotes(),
                order.getInternalNotes(),
                order.getCouponCode(),
                order.getCouponDiscount(),
                order.getRefundAmount(),
                order.getCancellationReason(),
                order.getCancelledAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                items,
                payments,
                statusHistory,
                allowedTransitions,
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
