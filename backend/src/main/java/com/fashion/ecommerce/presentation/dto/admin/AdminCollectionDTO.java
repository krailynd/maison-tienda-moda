package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Collection;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Admin collection detail (slice D). Carries the resolved season reference
 * plus the collection's own display window and timestamps.
 */
public record AdminCollectionDTO(
        Long id,
        String name,
        String slug,
        String description,
        String bannerUrl,
        Long seasonId,
        String seasonName,
        boolean isFeatured,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminCollectionDTO from(Collection c) {
        return new AdminCollectionDTO(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getDescription(),
                c.getBannerUrl(),
                c.getSeason() != null ? c.getSeason().getId() : null,
                c.getSeason() != null ? c.getSeason().getName() : null,
                Boolean.TRUE.equals(c.getIsFeatured()),
                c.getStartDate(),
                c.getEndDate(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
