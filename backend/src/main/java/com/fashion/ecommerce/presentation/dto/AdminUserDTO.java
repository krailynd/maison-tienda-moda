package com.fashion.ecommerce.presentation.dto;

import java.util.List;

/**
 * Authenticated user payload embedded in {@link AuthResponse} (roles use the
 * bare role name, e.g. {@code ADMIN}, without the {@code ROLE_} prefix).
 */
public record AdminUserDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        List<String> roles) {
}
