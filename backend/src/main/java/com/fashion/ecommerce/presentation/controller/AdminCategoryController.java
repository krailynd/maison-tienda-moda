package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminCategoryService;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminCategoryDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminCategoryRequest;
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
 * Admin category endpoints (slice D), secured by {@code /api/v1/admin/**} →
 * {@code ROLE_ADMIN}. Thin controller over {@link AdminCategoryService}.
 * <p>
 * List responses carry the {@code X-Total-Count} header and include inactive
 * categories; the body envelope is the standard
 * {@code {content, page, size, totalElements, totalPages}}.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public PageResponse<AdminCategoryDTO> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletResponse response) {
        PageResponse<AdminCategoryDTO> result = adminCategoryService.getCategories(page, size);
        response.setHeader("X-Total-Count", String.valueOf(result.totalElements()));
        return result;
    }

    @GetMapping("/{id}")
    public AdminCategoryDTO getCategory(@PathVariable Long id) {
        return adminCategoryService.getCategory(id);
    }

    @PostMapping
    public ResponseEntity<AdminCategoryDTO> createCategory(
            @Valid @RequestBody AdminCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public AdminCategoryDTO updateCategory(@PathVariable Long id,
                                           @Valid @RequestBody AdminCategoryRequest request) {
        return adminCategoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
