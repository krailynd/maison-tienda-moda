package com.fashion.ecommerce.presentation.dto;

import java.util.Map;

/**
 * Customer / shipping address echoed back on {@code GET /api/v1/orders/{orderNumber}}.
 */
public record CustomerResponse(
        String firstName,
        String lastName,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String countryCode) {

    public static CustomerResponse fromJson(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return new CustomerResponse(
                str(json, "firstName"),
                str(json, "lastName"),
                str(json, "email"),
                str(json, "phone"),
                str(json, "addressLine1"),
                str(json, "addressLine2"),
                str(json, "city"),
                str(json, "region"),
                str(json, "postalCode"),
                str(json, "countryCode")
        );
    }

    private static String str(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
