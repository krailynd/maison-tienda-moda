package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.OrderItem;

import java.math.BigDecimal;

/**
 * Order line for the admin order detail. Uses only columns frozen at order
 * time plus the variant id — it deliberately avoids walking the
 * variant → product → images lazy graph that the storefront
 * {@link com.fashion.ecommerce.presentation.dto.OrderItemResponse} needs for
 * its image URL.
 */
public record AdminOrderItemDTO(
        Long id,
        Long variantId,
        String productName,
        String variantName,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalPrice,
        String fulfillmentStatus,
        Integer shippedQuantity,
        Integer returnedQuantity) {

    public static AdminOrderItemDTO from(OrderItem item) {
        return new AdminOrderItemDTO(
                item.getId(),
                item.getVariant() != null ? item.getVariant().getId() : null,
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountAmount(),
                item.getTaxAmount(),
                item.getTotalPrice(),
                item.getFulfillmentStatus(),
                item.getShippedQuantity(),
                item.getReturnedQuantity());
    }
}
