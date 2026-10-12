package com.fashion.ecommerce.presentation.dto;

import java.math.BigDecimal;

/**
 * Response of {@code POST /api/v1/checkout/orders}.
 */
public record OrderCreatedResponse(
        Long id,
        String orderNumber,
        String status,
        BigDecimal total,
        String currency,
        String paymentIntent) {
}
