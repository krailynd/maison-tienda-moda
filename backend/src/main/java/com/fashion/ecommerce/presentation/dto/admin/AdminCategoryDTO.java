package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Category;

import java.time.LocalDateTime;

/**
 * Admin category detail (slice D). Richer than the storefront
 * {@link com.fashion.ecommerce.presentation.dto.CategoryDTO}: carries the
 * parent reference, activation flag, icon class, SEO fields and timestamps so
 * the admin list/editor can manage the whole tree including inactive rows.
 */
public record AdminCategoryDTO(
        Long id,
        String name,
        String slug,
        String description,
        Long parentId,
        String parentName,
        String imageUrl,
        boolean isActive,
        String iconClass,
        Integer displayOrder,
        String seoTitle,
        String seoDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminCategoryDTO from(Category c) {
        Category parent = c.getParent();
        return new AdminCategoryDTO(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getDescription(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                c.getImageUrl(),
                Boolean.TRUE.equals(c.getIsActive()),
                c.getIconClass(),
                c.getDisplayOrder(),
                c.getSeoTitle(),
                c.getSeoDescription(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
