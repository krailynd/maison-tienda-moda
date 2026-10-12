package com.fashion.ecommerce.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Line item of the checkout order request.
 * <p>
 * Prices are deliberately NOT accepted: totals are computed server-side from
 * the persisted variant price so the client cannot tamper with amounts.
 */
public record OrderItemRequest(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "variantId is required") Long variantId,
        @NotNull(message = "quantity is required") Integer quantity) {
}
