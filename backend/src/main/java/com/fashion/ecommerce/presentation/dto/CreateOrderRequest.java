package com.fashion.ecommerce.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Checkout order request body: {@code {items, customer, shippingMethodId?, notes?}}.
 */
public record CreateOrderRequest(
        @NotEmpty(message = "items must not be empty") List<@Valid OrderItemRequest> items,
        @NotNull(message = "customer is required") @Valid CustomerRequest customer,
        Long shippingMethodId,
        String notes) {
}
