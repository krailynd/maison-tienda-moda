package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Brand;

import java.time.LocalDateTime;

/**
 * Admin brand detail (slice D). Richer than the storefront
 * {@link com.fashion.ecommerce.presentation.dto.BrandDTO}: carries website
 * URL, activation/featured flags, display order and timestamps.
 */
public record AdminBrandDTO(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String websiteUrl,
        boolean isActive,
        boolean isFeatured,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminBrandDTO from(Brand b) {
        return new AdminBrandDTO(
                b.getId(),
                b.getName(),
                b.getSlug(),
                b.getDescription(),
                b.getLogoUrl(),
                b.getWebsiteUrl(),
                Boolean.TRUE.equals(b.getIsActive()),
                Boolean.TRUE.equals(b.getIsFeatured()),
                b.getDisplayOrder(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }
}
