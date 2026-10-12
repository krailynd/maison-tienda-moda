package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Brand;
import com.fashion.ecommerce.domain.model.Category;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductImage;
import com.fashion.ecommerce.domain.model.ProductVariant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Product card summary used by the list / related endpoints.
 * <p>
 * Field names intentionally match the storefront {@code Product} interface in
 * {@code src/lib/data.ts} (id, slug, name, price, originalPrice, category,
 * image, colors, sizes, rating, reviews, isNew, isSale, description, details)
 * so the mock-data swap is a drop-in replacement.
 */
public record ProductSummaryDTO(
        String id,
        String slug,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        String currency,
        String image,
        List<String> images,
        String category,
        String categoryName,
        String categoryId,
        BrandRefDTO brand,
        List<ColorDTO> colors,
        List<SizeDTO> sizes,
        double rating,
        int reviews,
        boolean isNew,
        boolean isSale,
        String description,
        List<String> details,
        boolean inStock) {

    public static ProductSummaryDTO from(Product p, List<ProductImage> images,
                                         List<ProductVariant> variants,
                                         Category category, Brand brand) {
        ProductImage primary = images.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
        String primaryImage = primary != null ? primary.getImageUrl() : null;
        List<String> imageUrls = images.stream().map(ProductImage::getImageUrl).toList();

        Map<String, ColorDTO> colors = new LinkedHashMap<>();
        Map<String, Boolean> sizes = new LinkedHashMap<>();
        boolean anyInStock = false;
        for (ProductVariant v : variants.stream().distinct().toList()) {
            if (Boolean.FALSE.equals(v.getIsActive())) {
                continue;
            }
            if (v.getColor() != null) {
                colors.putIfAbsent(v.getColor().getName(),
                        new ColorDTO(v.getColor().getName(), v.getColor().getHexCode()));
            }
            String sizeLabel = v.getSize() != null ? v.getSize().getCode() : "Único";
            boolean inStock = VariantDTO.totalStock(v) > 0;
            sizes.merge(sizeLabel, inStock, Boolean::logicalOr);
            anyInStock = anyInStock || inStock;
        }

        return new ProductSummaryDTO(
                String.valueOf(p.getId()),
                p.getSlug(),
                p.getName(),
                p.getPrice(),
                p.getComparePrice(),
                "PEN",
                primaryImage,
                imageUrls,
                category != null ? category.getSlug() : null,
                category != null ? category.getName() : null,
                category != null ? String.valueOf(category.getId()) : null,
                brand != null ? new BrandRefDTO(String.valueOf(brand.getId()), brand.getName(), brand.getSlug()) : null,
                new ArrayList<>(colors.values()),
                sizes.entrySet().stream().map(e -> new SizeDTO(e.getKey(), e.getValue())).toList(),
                p.getAverageRating() == null ? 0.0 : p.getAverageRating().doubleValue(),
                p.getReviewCount() == null ? 0 : p.getReviewCount(),
                Boolean.TRUE.equals(p.getIsNew()),
                p.hasDiscount(),
                p.getDescription(),
                buildDetails(p),
                anyInStock
        );
    }

    /**
     * The schema has no {@code details} column; derive a bullet list from the
     * description so the storefront "Detalles y cuidados" accordion stays wired.
     */
    private static List<String> buildDetails(Product p) {
        List<String> details = new ArrayList<>();
        if (p.getDescription() != null) {
            for (String part : p.getDescription().split("\\.\\s+")) {
                String trimmed = part.trim().replaceAll("\\.$", "");
                if (!trimmed.isEmpty()) {
                    details.add(trimmed);
                }
            }
        }
        if (details.isEmpty() && p.getShortDescription() != null && !p.getShortDescription().isBlank()) {
            details.add(p.getShortDescription().trim());
        }
        return details;
    }
}
