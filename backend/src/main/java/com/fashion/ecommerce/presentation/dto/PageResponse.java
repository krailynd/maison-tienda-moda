package com.fashion.ecommerce.presentation.dto;

/**
 * Paginated envelope used by catalog list endpoints.
 * <p>
 * Matches {@code {content, page, size, totalElements, totalPages}} plus the
 * {@code X-Total-Count} header set by the controller.
 */
public record PageResponse<T>(java.util.List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<?> source, java.util.List<T> content) {
        return new PageResponse<>(content, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
