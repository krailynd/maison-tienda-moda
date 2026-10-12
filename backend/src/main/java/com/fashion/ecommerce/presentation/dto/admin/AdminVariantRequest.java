package com.fashion.ecommerce.presentation.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Admin variant write payload (create/update).
 * <p>
 * {@code stock} is the quantity on hand for the variant's default-warehouse
 * inventory row (the domain models stock on {@code Inventory}, not on the
 * variant itself — the service maps it there).
 */
public record AdminVariantRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must be at most 100 characters")
        String sku,

        Long sizeId,

        Long colorId,

        @DecimalMin(value = "0.0", message = "Price must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Compare price must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Compare price must have at most 8 integer and 2 decimal digits")
        BigDecimal comparePrice,

        @DecimalMin(value = "0.0", message = "Cost price must be zero or greater")
        @Digits(integer = 8, fraction = 2, message = "Cost price must have at most 8 integer and 2 decimal digits")
        BigDecimal costPrice,

        Boolean isDefault,
        Boolean isActive,
        Integer position,

        @Min(value = 0, message = "Stock must be zero or greater")
        Integer stock) {
}
