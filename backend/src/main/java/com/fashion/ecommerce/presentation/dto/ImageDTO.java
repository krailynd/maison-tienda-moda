package com.fashion.ecommerce.presentation.dto;

/**
 * Image exposed in the product detail gallery.
 */
public record ImageDTO(Long id, String url, String thumbnailUrl, String altText,
                       boolean primary, Integer position) {
}
