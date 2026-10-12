package com.fashion.ecommerce.presentation.dto;

import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductImage;
import com.fashion.ecommerce.domain.model.ProductVariant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full product detail for {@code GET /api/v1/products/{slugOrId}}.
 * <p>
 * Reuses the card fields of {@link ProductSummaryDTO} (so the storefront's
 * {@code Product} interface keeps working) and adds the gallery, per-variant
 * inventory, brand and category objects.
 */
public record ProductDetailDTO(
        String id,
        String slug,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        String currency,
        String image,
        List<ImageDTO> images,
        String description,
        String shortDescription,
        List<String> details,
        String category,
        String categoryName,
        String categoryId,
        BrandRefDTO brand,
        List<ColorDTO> colors,
        List<SizeDTO> sizes,
        List<VariantDTO> variants,
        double rating,
        int reviews,
        boolean isNew,
        boolean isSale,
        boolean inStock,
        LocalDateTime createdAt) {

    public static ProductDetailDTO from(Product p) {
        List<ProductImage> images = p.getImages() != null
                ? p.getImages().stream().distinct().toList()
                : List.of();
        ProductImage primary = images.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));

        List<ProductVariant> variants = p.getVariants() != null
                ? p.getVariants().stream().distinct().toList()
                : List.of();
        Map<String, ColorDTO> colors = new LinkedHashMap<>();
        Map<String, Boolean> sizes = new LinkedHashMap<>();
        boolean anyInStock = false;
        for (ProductVariant v : variants) {
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

        boolean isSale = p.hasDiscount();
        return new ProductDetailDTO(
                String.valueOf(p.getId()),
                p.getSlug(),
                p.getName(),
                p.getPrice(),
                p.getComparePrice(),
                "PEN",
                primary != null ? primary.getImageUrl() : null,
                images.stream().map(i -> new ImageDTO(i.getId(), i.getImageUrl(), i.getThumbnailUrl(),
                        i.getAltText(), Boolean.TRUE.equals(i.getIsPrimary()), i.getPosition())).toList(),
                p.getDescription(),
                p.getShortDescription(),
                buildDetails(p),
                p.getCategory() != null ? p.getCategory().getSlug() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getCategory() != null ? String.valueOf(p.getCategory().getId()) : null,
                p.getBrand() != null
                        ? new BrandRefDTO(String.valueOf(p.getBrand().getId()), p.getBrand().getName(), p.getBrand().getSlug())
                        : null,
                new ArrayList<>(colors.values()),
                sizes.entrySet().stream().map(e -> new SizeDTO(e.getKey(), e.getValue())).toList(),
                variants.stream().map(v -> VariantDTO.from(v, p)).toList(),
                p.getAverageRating() == null ? 0.0 : p.getAverageRating().doubleValue(),
                p.getReviewCount() == null ? 0 : p.getReviewCount(),
                Boolean.TRUE.equals(p.getIsNew()),
                isSale,
                anyInStock,
                p.getCreatedAt()
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
