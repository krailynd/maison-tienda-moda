package com.fashion.ecommerce.presentation.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Admin collection write payload (create/update, slice D). {@code seasonId}
 * references the seeded {@code seasons} table (PV2026 / OI2026).
 */
public record AdminCollectionRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 150, message = "Slug must be at most 150 characters")
        String slug,

        String description,

        String bannerUrl,

        Long seasonId,

        Boolean isFeatured,

        LocalDate startDate,

        LocalDate endDate) {
}
