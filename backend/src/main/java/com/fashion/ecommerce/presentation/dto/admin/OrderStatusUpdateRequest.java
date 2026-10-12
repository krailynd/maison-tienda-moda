package com.fashion.ecommerce.presentation.dto.admin;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Body of {@code POST /api/v1/admin/orders/{id}/status}.
 * <p>
 * {@code newStatus} must be a legal successor of the order's current status
 * (see {@code Order.canTransitionTo}); unknown enum names are rejected with
 * 400, illegal transitions with 409. {@code notes} doubles as the
 * cancellation reason for {@code CANCELLED} and {@code refundAmount} is
 * persisted for {@code REFUNDED}.
 */
public record OrderStatusUpdateRequest(
        @NotBlank(message = "newStatus is required") String newStatus,
        String notes,
        BigDecimal refundAmount) {
}
