package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.ProductImage;

/**
 * Admin image detail for the slice C image endpoints.
 * <p>
 * {@code thumbnailUrl} is intentionally nullable: thumbnail generation is
 * deferred (design D4) and is NOT part of this slice.
 */
public record AdminImageDTO(Long id, String imageUrl, String thumbnailUrl, String altText,
                            Integer position, boolean isPrimary) {

    public static AdminImageDTO from(ProductImage image) {
        return new AdminImageDTO(image.getId(), image.getImageUrl(), image.getThumbnailUrl(),
                image.getAltText(), image.getPosition(), Boolean.TRUE.equals(image.getIsPrimary()));
    }
}
