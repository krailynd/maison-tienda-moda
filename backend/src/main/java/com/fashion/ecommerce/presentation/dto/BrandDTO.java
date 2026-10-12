package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Brand;

/**
 * Brand summary for {@code GET /api/v1/brands}.
 */
public record BrandDTO(
        String id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String websiteUrl,
        boolean isFeatured,
        Integer displayOrder) {

    public static BrandDTO from(Brand b) {
        return new BrandDTO(
                String.valueOf(b.getId()),
                b.getName(),
                b.getSlug(),
                b.getDescription(),
                b.getLogoUrl(),
                b.getWebsiteUrl(),
                Boolean.TRUE.equals(b.getIsFeatured()),
                b.getDisplayOrder()
        );
    }
}
