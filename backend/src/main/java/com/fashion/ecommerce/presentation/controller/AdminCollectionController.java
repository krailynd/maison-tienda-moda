package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminCollectionService;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminCollectionDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminCollectionRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin collection endpoints (slice D), secured by {@code /api/v1/admin/**} →
 * {@code ROLE_ADMIN}. Thin controller over {@link AdminCollectionService}.
 * <p>
 * List responses carry the {@code X-Total-Count} header; the body envelope is
 * the standard {@code {content, page, size, totalElements, totalPages}}.
 */
@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
public class AdminCollectionController {

    private final AdminCollectionService adminCollectionService;

    @GetMapping
    public PageResponse<AdminCollectionDTO> getCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletResponse response) {
        PageResponse<AdminCollectionDTO> result = adminCollectionService.getCollections(page, size);
        response.setHeader("X-Total-Count", String.valueOf(result.totalElements()));
        return result;
    }

    @GetMapping("/{id}")
    public AdminCollectionDTO getCollection(@PathVariable Long id) {
        return adminCollectionService.getCollection(id);
    }

    @PostMapping
    public ResponseEntity<AdminCollectionDTO> createCollection(
            @Valid @RequestBody AdminCollectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCollectionService.createCollection(request));
    }

    @PutMapping("/{id}")
    public AdminCollectionDTO updateCollection(@PathVariable Long id,
                                               @Valid @RequestBody AdminCollectionRequest request) {
        return adminCollectionService.updateCollection(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id) {
        adminCollectionService.deleteCollection(id);
        return ResponseEntity.noContent().build();
    }
}
