package com.fashion.ecommerce.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Customer / shipping address for the checkout order request. Stored as JSONB
 * on the {@code orders.shipping_address} / {@code billing_address} columns.
 */
public record CustomerRequest(
        @NotBlank(message = "firstName is required") String firstName,
        @NotBlank(message = "lastName is required") String lastName,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        String phone,
        @NotBlank(message = "addressLine1 is required") String addressLine1,
        String addressLine2,
        @NotBlank(message = "city is required") String city,
        String region,
        String postalCode,
        String countryCode) {
}
