package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.Season;

/**
 * Read-only season option for the admin collection editor (slice D).
 */
public record AdminSeasonOptionDTO(Long id, String name, String code, boolean isActive) {

    public static AdminSeasonOptionDTO from(Season s) {
        return new AdminSeasonOptionDTO(
                s.getId(),
                s.getName(),
                s.getCode(),
                Boolean.TRUE.equals(s.getIsActive()));
    }
}
