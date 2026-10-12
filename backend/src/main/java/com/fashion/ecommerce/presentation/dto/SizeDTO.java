package com.fashion.ecommerce.presentation.dto;

/**
 * Size availability exposed to the storefront. Matches the frontend
 * {@code sizes: { label, available }[]} shape from {@code data.ts}.
 */
public record SizeDTO(String label, boolean available) {
}
