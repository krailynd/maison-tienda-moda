package com.fashion.ecommerce.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Logout payload: {@code POST /api/v1/auth/logout} revokes the refresh-token jti.
 */
public record LogoutRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
