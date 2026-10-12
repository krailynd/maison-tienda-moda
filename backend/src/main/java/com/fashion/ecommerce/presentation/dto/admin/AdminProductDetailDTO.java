package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductImage;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fashion.ecommerce.presentation.dto.BrandRefDTO;
import com.fashion.ecommerce.presentation.dto.CategoryRefDTO;
import com.fashion.ecommerce.presentation.dto.ImageDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin product detail for {@code GET/POST/PUT /api/v1/admin/products[/{id}]}.
 * <p>
 * Images reuse the storefront's {@link ImageDTO} shape; variants use the
 * admin-only {@link AdminVariantDTO} (adds pricing, position and the color id
 * for the slice C editors). Variants/images are read-only here — editing them
 * lands in the slice C endpoints.
 */
public record AdminProductDetailDTO(
        Long id,
        String slug,
        String name,
        String description,
        String shortDescription,
        String skuPrefix,
        BigDecimal price,
        BigDecimal comparePrice,
        BigDecimal costPrice,
        BigDecimal taxRate,
        BigDecimal weight,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        boolean isActive,
        boolean isFeatured,
        boolean isNew,
        boolean isEcoFriendly,
        boolean stockTracking,
        Integer lowStockThreshold,
        Integer minOrderQuantity,
        Integer maxOrderQuantity,
        BigDecimal averageRating,
        Integer reviewCount,
        Integer viewCount,
        Integer purchaseCount,
        String seoTitle,
        String seoDescription,
        String metaKeywords,
        CategoryRefDTO category,
        BrandRefDTO brand,
        List<ImageDTO> images,
        List<AdminVariantDTO> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdminProductDetailDTO from(Product p) {
        List<ProductImage> images = p.getImages() != null
                ? p.getImages().stream().distinct().toList()
                : List.of();
        List<ProductVariant> variants = p.getVariants() != null
                ? p.getVariants().stream().distinct().toList()
                : List.of();

        return new AdminProductDetailDTO(
                p.getId(),
                p.getSlug(),
                p.getName(),
                p.getDescription(),
                p.getShortDescription(),
                p.getSkuPrefix(),
                p.getPrice(),
                p.getComparePrice(),
                p.getCostPrice(),
                p.getTaxRate(),
                p.getWeight(),
                p.getLengthCm(),
                p.getWidthCm(),
                p.getHeightCm(),
                Boolean.TRUE.equals(p.getIsActive()),
                Boolean.TRUE.equals(p.getIsFeatured()),
                Boolean.TRUE.equals(p.getIsNew()),
                Boolean.TRUE.equals(p.getIsEcoFriendly()),
                Boolean.TRUE.equals(p.getStockTracking()),
                p.getLowStockThreshold(),
                p.getMinOrderQuantity(),
                p.getMaxOrderQuantity(),
                p.getAverageRating(),
                p.getReviewCount(),
                p.getViewCount(),
                p.getPurchaseCount(),
                p.getSeoTitle(),
                p.getSeoDescription(),
                p.getMetaKeywords(),
                p.getCategory() != null
                        ? new CategoryRefDTO(String.valueOf(p.getCategory().getId()),
                        p.getCategory().getName(), p.getCategory().getSlug())
                        : null,
                p.getBrand() != null
                        ? new BrandRefDTO(String.valueOf(p.getBrand().getId()),
                        p.getBrand().getName(), p.getBrand().getSlug())
                        : null,
                images.stream().map(i -> new ImageDTO(i.getId(), i.getImageUrl(), i.getThumbnailUrl(),
                        i.getAltText(), Boolean.TRUE.equals(i.getIsPrimary()), i.getPosition())).toList(),
                variants.stream().map(AdminVariantDTO::from).toList(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
