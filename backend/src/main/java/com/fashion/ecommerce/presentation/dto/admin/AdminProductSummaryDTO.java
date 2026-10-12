package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductVariant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin product list row for {@code GET /api/v1/admin/products}.
 * <p>
 * Unlike the storefront card, this includes drafts and inactive products
 * (the admin manages the whole catalog, not just what is published).
 */
public record AdminProductSummaryDTO(
        Long id,
        String slug,
        String name,
        BigDecimal price,
        BigDecimal comparePrice,
        boolean isActive,
        boolean isFeatured,
        boolean isNew,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        int totalStock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminProductSummaryDTO from(Product p, List<ProductVariant> variants) {
        int totalStock = variants == null ? 0 : variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .mapToInt(ProductVariant::getTotalStock)
                .sum();
        return new AdminProductSummaryDTO(
                p.getId(),
                p.getSlug(),
                p.getName(),
                p.getPrice(),
                p.getComparePrice(),
                Boolean.TRUE.equals(p.getIsActive()),
                Boolean.TRUE.equals(p.getIsFeatured()),
                Boolean.TRUE.equals(p.getIsNew()),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getBrand() != null ? p.getBrand().getId() : null,
                p.getBrand() != null ? p.getBrand().getName() : null,
                totalStock,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
