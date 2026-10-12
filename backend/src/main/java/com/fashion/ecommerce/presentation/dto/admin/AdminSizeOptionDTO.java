package com.fashion.ecommerce.presentation.dto.admin;

/**
 * Size option for the admin variant editor ({@code GET /api/v1/admin/sizes}).
 */
public record AdminSizeOptionDTO(Long id, String name, String code) {
}
