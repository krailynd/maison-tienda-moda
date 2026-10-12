package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment info echoed on {@code GET /api/v1/orders/{orderNumber}}.
 */
public record OrderPaymentResponse(
        String provider,
        String method,
        String transactionId,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime processedAt) {

    public static OrderPaymentResponse from(Payment p) {
        return new OrderPaymentResponse(
                p.getPaymentProvider(),
                p.getPaymentMethod(),
                p.getTransactionId(),
                p.getAmount(),
                p.getCurrencyCode(),
                p.getStatus().name(),
                p.getProcessedAt()
        );
    }
}
