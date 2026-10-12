package com.fashion.ecommerce.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh-token rotation payload: {@code POST /api/v1/auth/refresh}.
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
