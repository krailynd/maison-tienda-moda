package com.fashion.ecommerce.presentation.dto.admin;

/**
 * Color reference for admin payloads. Unlike the storefront {@link
 * com.fashion.ecommerce.presentation.dto.ColorDTO}, it carries the id so the
 * admin variant editor can prefill its color select.
 */
public record AdminColorRefDTO(Long id, String name, String hexCode) {
}
