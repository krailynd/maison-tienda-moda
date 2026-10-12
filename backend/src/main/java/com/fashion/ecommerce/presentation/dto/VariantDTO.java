package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Inventory;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductVariant;

import java.math.BigDecimal;
import java.util.List;

/**
 * Variant exposed in the product detail. Carries the size/color selectors plus
 * the server-side inventory so the storefront can resolve a {@code variantId}
 * when the customer picks a size + color at checkout.
 */
public record VariantDTO(Long id, String name, String sku, BigDecimal price,
                         SizeRefDTO size, ColorDTO color, int stock,
                         boolean inStock, boolean isDefault, boolean isActive) {

    /**
     * Sum of available stock across the variant's inventory rows. {@code
     * distinct()} guards against duplicate bag elements when inventories were
     * fetched through an entity graph join.
     */
    static int totalStock(ProductVariant v) {
        if (v.getInventories() == null || v.getInventories().isEmpty()) {
            return 0;
        }
        return v.getInventories().stream()
                .distinct()
                .mapToInt(Inventory::getQuantityAvailable)
                .sum();
    }

    public static VariantDTO from(ProductVariant v, Product product) {
        int stock = totalStock(v);
        return new VariantDTO(
                v.getId(),
                v.getName(),
                v.getSku(),
                v.getPrice() != null ? v.getPrice() : product.getPrice(),
                v.getSize() != null ? new SizeRefDTO(v.getSize().getId(), v.getSize().getName(), v.getSize().getCode()) : null,
                v.getColor() != null ? new ColorDTO(v.getColor().getName(), v.getColor().getHexCode()) : null,
                stock,
                stock > 0,
                Boolean.TRUE.equals(v.getIsDefault()),
                Boolean.TRUE.equals(v.getIsActive())
        );
    }
}
