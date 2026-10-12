package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.OrderItem;

import java.math.BigDecimal;

/**
 * Order line snapshot for the success page. Product/variant names are the
 * columns frozen at order time (not live lookups).
 */
public record OrderItemResponse(
        Long productId,
        Long variantId,
        String productName,
        String variantName,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String imageUrl) {

    public static OrderItemResponse from(OrderItem item, String imageUrl) {
        return new OrderItemResponse(
                item.getVariant().getProduct().getId(),
                item.getVariant().getId(),
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                imageUrl
        );
    }
}
