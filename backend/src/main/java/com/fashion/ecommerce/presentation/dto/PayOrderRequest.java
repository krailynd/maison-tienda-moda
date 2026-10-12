package com.fashion.ecommerce.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/v1/checkout/orders/{orderNumber}/pay}.
 * <p>
 * MVP: accepts {@code {method: "card"}} and simulates success locally instead of
 * calling a real gateway.
 */
public record PayOrderRequest(@NotBlank(message = "method is required") String method) {
}
