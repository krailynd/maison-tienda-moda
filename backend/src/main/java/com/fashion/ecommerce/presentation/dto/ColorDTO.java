package com.fashion.ecommerce.presentation.dto;

/**
 * Color exposed to the storefront. Matches the frontend
 * {@code colors: { name, hex }[]} shape from {@code data.ts}.
 */
public record ColorDTO(String name, String hex) {
}
