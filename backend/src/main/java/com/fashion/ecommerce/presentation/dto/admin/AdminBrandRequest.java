package com.fashion.ecommerce.presentation.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin brand write payload (create/update, slice D).
 */
public record AdminBrandRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 100, message = "Slug must be at most 100 characters")
        String slug,

        String description,

        String logoUrl,

        @Size(max = 255, message = "Website URL must be at most 255 characters")
        String websiteUrl,

        Boolean isActive,

        Boolean isFeatured,

        Integer displayOrder) {
}
