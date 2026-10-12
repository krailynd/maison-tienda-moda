package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Order;
import com.fashion.ecommerce.domain.model.OrderItem;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductImage;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Order detail for {@code GET /api/v1/orders/{orderNumber}} (success page):
 * totals, item snapshot, customer, and payment info.
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        String status,
        String paymentStatus,
        String currency,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingCost,
        BigDecimal tax,
        BigDecimal total,
        LocalDateTime createdAt,
        String shippingMethod,
        CustomerResponse customer,
        List<OrderItemResponse> items,
        OrderPaymentResponse payment) {

    public static OrderResponse from(Order order, ObjectMapper objectMapper) {
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                        .distinct()
                        .map(item -> OrderItemResponse.from(item, resolvePrimaryImage(item)))
                        .toList();

        OrderPaymentResponse payment = order.getPayments() == null || order.getPayments().isEmpty()
                ? null
                : order.getPayments().stream()
                        .distinct()
                        .findFirst()
                        .map(OrderPaymentResponse::from)
                        .orElse(null);

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getCurrencyCode(),
                order.getSubTotal(),
                order.getDiscountAmount(),
                order.getShippingCost(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getShippingMethod(),
                CustomerResponse.fromJson(readJson(order.getShippingAddress(), objectMapper)),
                items,
                payment
        );
    }

    private static String resolvePrimaryImage(OrderItem item) {
        try {
            ProductVariant variant = item.getVariant();
            if (variant == null) {
                return null;
            }
            Product product = variant.getProduct();
            if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
                return null;
            }
            return product.getImages().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                    .findFirst()
                    .orElse(product.getImages().get(0))
                    .getImageUrl();
        } catch (RuntimeException ex) {
            return null;
        }
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
}
