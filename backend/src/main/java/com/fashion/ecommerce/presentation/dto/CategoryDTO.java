package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Category;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Category with a product count for the catalog sidebar.
 * <p>
 * Exposes the fields requested by the task (id, name, slug, icon, sortOrder,
 * productCount) plus the storefront tile fields (imageUrl, description) and the
 * tree structure (parentId, children).
 */
public record CategoryDTO(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String icon,
        Integer sortOrder,
        long productCount,
        String parentId,
        List<CategoryDTO> children) {

    public static CategoryDTO from(Category c, Map<Long, Long> counts, Map<Long, List<Category>> byParent) {
        long count = counts.getOrDefault(c.getId(), 0L);
        List<CategoryDTO> children = byParent.getOrDefault(c.getId(), List.of()).stream()
                .sorted(Comparator.comparing(Category::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(child -> from(child, counts, byParent))
                .toList();
        String parentId = c.getParent() != null ? String.valueOf(c.getParent().getId()) : null;
        return new CategoryDTO(
                String.valueOf(c.getId()),
                c.getName(),
                c.getSlug(),
                c.getDescription(),
                c.getImageUrl(),
                c.getIconClass(),
                c.getDisplayOrder(),
                count,
                parentId,
                children
        );
    }
}
