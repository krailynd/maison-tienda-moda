package com.fashion.ecommerce.presentation.dto;

/**
 * Authentication response for {@code /api/v1/auth/login} and
 * {@code /api/v1/auth/refresh}: an access/refresh token pair plus the user.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AdminUserDTO user) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, AdminUserDTO user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
