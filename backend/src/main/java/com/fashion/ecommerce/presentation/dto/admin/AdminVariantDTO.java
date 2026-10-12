package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Inventory;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fashion.ecommerce.presentation.dto.SizeRefDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin variant detail for the slice C endpoints. Richer than the storefront
 * {@link com.fashion.ecommerce.presentation.dto.VariantDTO}: carries pricing,
 * position, the color id and the live stock total.
 */
public record AdminVariantDTO(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        BigDecimal comparePrice,
        BigDecimal costPrice,
        SizeRefDTO size,
        AdminColorRefDTO color,
        int stock,
        boolean inStock,
        boolean isDefault,
        boolean isActive,
        Integer position,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    static int totalStock(ProductVariant v) {
        if (v.getInventories() == null || v.getInventories().isEmpty()) {
            return 0;
        }
        return v.getInventories().stream()
                .distinct()
                .mapToInt(Inventory::getQuantityAvailable)
                .sum();
    }

    public static AdminVariantDTO from(ProductVariant v) {
        int stock = totalStock(v);
        return new AdminVariantDTO(
                v.getId(),
                v.getName(),
                v.getSku(),
                v.getPrice(),
                v.getComparePrice(),
                v.getCostPrice(),
                v.getSize() != null
                        ? new SizeRefDTO(v.getSize().getId(), v.getSize().getName(), v.getSize().getCode())
                        : null,
                v.getColor() != null
                        ? new AdminColorRefDTO(v.getColor().getId(), v.getColor().getName(), v.getColor().getHexCode())
                        : null,
                stock,
                stock > 0,
                Boolean.TRUE.equals(v.getIsDefault()),
                Boolean.TRUE.equals(v.getIsActive()),
                v.getPosition(),
                v.getCreatedAt(),
                v.getUpdatedAt());
    }
}
