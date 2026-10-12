package com.fashion.ecommerce.presentation.dto;

import java.time.LocalDateTime;

/**
 * Response of {@code POST /api/v1/checkout/orders/{orderNumber}/pay}.
 * <p>
 * {@code status} is the order payment status ("PAID") after the simulated
 * success; {@code paidAt} is when the payment row was processed.
 */
public record PaymentResultResponse(
        String status,
        LocalDateTime paidAt,
        String transactionId,
        String orderNumber) {
}
