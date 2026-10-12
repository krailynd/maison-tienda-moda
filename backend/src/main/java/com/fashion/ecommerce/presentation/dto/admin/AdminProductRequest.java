package com.fashion.ecommerce.presentation.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Admin product write payload (create/update).
 * <p>
 * {@code isActive} is intentionally absent: products are always created as
 * drafts (CAT-4) and visibility is toggled through
 * {@code POST /admin/products/{id}/publish|unpublish} only.
 */
public record AdminProductRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase letters, numbers and hyphens")
        @Size(max = 255, message = "Slug must be at most 255 characters")
        String slug,

        String description,

        @Size(max = 500, message = "Short description must be at most 500 characters")
        String shortDescription,

        @Size(max = 50, message = "SKU prefix must be at most 50 characters")
        String skuPrefix,

        Long brandId,

        Long categoryId,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Compare price must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Compare price must have at most 8 integer and 2 decimal digits")
        BigDecimal comparePrice,

        @DecimalMin(value = "0.0", message = "Cost price must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Cost price must have at most 8 integer and 2 decimal digits")
        BigDecimal costPrice,

        @DecimalMin(value = "0.0", message = "Tax rate must be zero or greater")
        @Digits(integer = 5, fraction = 2, message = "Tax rate must have at most 5 integer and 2 decimal digits")
        BigDecimal taxRate,

        @DecimalMin(value = "0.0", message = "Weight must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Weight must have at most 8 integer and 2 decimal digits")
        BigDecimal weight,

        @DecimalMin(value = "0.0", message = "Length must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Length must have at most 8 integer and 2 decimal digits")
        BigDecimal lengthCm,

        @DecimalMin(value = "0.0", message = "Width must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Width must have at most 8 integer and 2 decimal digits")
        BigDecimal widthCm,

        @DecimalMin(value = "0.0", message = "Height must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Height must have at most 8 integer and 2 decimal digits")
        BigDecimal heightCm,

        Boolean isFeatured,
        Boolean isNew,
        Boolean isEcoFriendly,
        Boolean stockTracking,
        Integer lowStockThreshold,
        Integer minOrderQuantity,
        Integer maxOrderQuantity,

        @Size(max = 255, message = "SEO title must be at most 255 characters")
        String seoTitle,
        String seoDescription,
        String metaKeywords) {
}
