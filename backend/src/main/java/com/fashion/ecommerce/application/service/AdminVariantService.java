package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Color;
import com.fashion.ecommerce.domain.model.Inventory;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fashion.ecommerce.domain.model.Size;
import com.fashion.ecommerce.domain.model.Warehouse;
import com.fashion.ecommerce.domain.repository.ColorRepository;
import com.fashion.ecommerce.domain.repository.InventoryRepository;
import com.fashion.ecommerce.domain.repository.ProductRepository;
import com.fashion.ecommerce.domain.repository.ProductVariantRepository;
import com.fashion.ecommerce.domain.repository.SizeRepository;
import com.fashion.ecommerce.domain.repository.WarehouseRepository;
import com.fashion.ecommerce.presentation.dto.admin.AdminVariantDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminVariantRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Admin write side of product variants (slice C).
 * <p>
 * Stock is modelled on {@link Inventory} rows, not on the variant: the request
 * {@code stock} field maps to the quantity on hand in the variant's
 * default-warehouse row (the seeded {@code WH-001} "Almacén Central", falling
 * back to the first active warehouse). Stock in other warehouses is left
 * untouched.
 * <p>
 * Error contract mirrors {@link AdminProductService}: missing product/variant →
 * 404, duplicate SKU → 409 ({@link IllegalStateException}), unknown size/color
 * → 400 ({@link IllegalArgumentException}).
 */
@Service
@RequiredArgsConstructor
public class AdminVariantService {

    /** Seeded default warehouse (V1): "Almacén Central". */
    private static final String DEFAULT_WAREHOUSE_CODE = "WH-001";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;

    @Transactional
    public AdminVariantDTO createVariant(Long productId, AdminVariantRequest request) {
        Product product = findProduct(productId);
        validateSkuAvailable(request.sku(), null);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(request.name())
                .sku(request.sku().trim())
                .size(resolveSize(request.sizeId()))
                .color(resolveColor(request.colorId()))
                .price(request.price())
                .comparePrice(request.comparePrice())
                .costPrice(request.costPrice())
                .isDefault(Boolean.TRUE.equals(request.isDefault()))
                .isActive(request.isActive() == null || request.isActive())
                .position(request.position() != null ? request.position() : nextPosition(product))
                .build();
        if (Boolean.TRUE.equals(request.isDefault())) {
            demoteDefaultVariants(productId);
        }
        variantRepository.save(variant);
        upsertStock(variant, request.stock());
        return AdminVariantDTO.from(variant);
    }

    @Transactional
    public AdminVariantDTO updateVariant(Long variantId, AdminVariantRequest request) {
        ProductVariant variant = findVariant(variantId);
        validateSkuAvailable(request.sku(), variantId);

        variant.setName(request.name());
        variant.setSku(request.sku().trim());
        variant.setSize(resolveSize(request.sizeId()));
        variant.setColor(resolveColor(request.colorId()));
        variant.setPrice(request.price());
        variant.setComparePrice(request.comparePrice());
        variant.setCostPrice(request.costPrice());
        if (request.isDefault() != null) {
            variant.setIsDefault(request.isDefault());
            if (request.isDefault()) {
                demoteDefaultVariants(variant.getProduct().getId());
            }
        }
        if (request.isActive() != null) {
            variant.setIsActive(request.isActive());
        }
        if (request.position() != null) {
            variant.setPosition(request.position());
        }
        upsertStock(variant, request.stock());
        return AdminVariantDTO.from(variant);
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        // Inventory rows are hard-deleted (inventories has no soft-delete
        // column); the variant itself is soft-deleted via @SQLDelete.
        inventoryRepository.deleteByVariantId(variantId);
        variantRepository.delete(findVariant(variantId));
    }

    // ---------- internals ----------

    private Product findProduct(Long productId) {
        return productRepository.findWithDetailsById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productId));
    }

    private ProductVariant findVariant(Long variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Variant not found: " + variantId));
    }

    /**
     * A SKU already owned by another (non-deleted) variant is a duplicate →
     * 409 via {@link IllegalStateException} (same contract as product slugs).
     */
    private void validateSkuAvailable(String sku, Long excludeId) {
        boolean taken = excludeId != null
                ? variantRepository.existsBySkuAndIdNot(sku, excludeId)
                : variantRepository.existsBySku(sku);
        if (taken) {
            throw new IllegalStateException("SKU already in use: " + sku);
        }
    }

    private void demoteDefaultVariants(Long productId) {
        for (ProductVariant v : variantRepository.findByProductId(productId)) {
            if (Boolean.TRUE.equals(v.getIsDefault())) {
                v.setIsDefault(false);
            }
        }
    }

    private int nextPosition(Product product) {
        return product.getVariants().stream()
                .mapToInt(v -> v.getPosition() != null ? v.getPosition() : 0)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Maps the request stock to the variant's default-warehouse inventory row
     * (upsert). {@code null} stock leaves inventory untouched.
     */
    private void upsertStock(ProductVariant variant, Integer stock) {
        if (stock == null) {
            return;
        }
        Warehouse warehouse = resolveDefaultWarehouse();
        Inventory inventory = inventoryRepository
                .findByVariantIdAndWarehouseId(variant.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .variant(variant)
                        .warehouse(warehouse)
                        .quantityReserved(0)
                        .build());
        inventory.setQuantityOnHand(stock);
        // Keep the entity graph consistent so the response DTO sees the row:
        // a brand-new row must be registered in the variant's collection.
        if (inventory.getId() == null) {
            variant.getInventories().add(inventory);
        }
        inventoryRepository.save(inventory);
    }

    private Warehouse resolveDefaultWarehouse() {
        return warehouseRepository.findByCode(DEFAULT_WAREHOUSE_CODE)
                .orElseGet(() -> warehouseRepository.findFirstByIsActiveTrueOrderByIdAsc()
                        .orElseThrow(() -> new IllegalStateException("No active warehouse configured for stock")));
    }

    private Size resolveSize(Long sizeId) {
        if (sizeId == null) {
            return null;
        }
        return sizeRepository.findById(sizeId)
                .orElseThrow(() -> new IllegalArgumentException("Size not found: " + sizeId));
    }

    private Color resolveColor(Long colorId) {
        if (colorId == null) {
            return null;
        }
        return colorRepository.findById(colorId)
                .orElseThrow(() -> new IllegalArgumentException("Color not found: " + colorId));
    }
}
