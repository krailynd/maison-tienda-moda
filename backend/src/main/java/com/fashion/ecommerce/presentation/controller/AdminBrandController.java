package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminBrandService;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminBrandDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminBrandRequest;
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
 * Admin brand endpoints (slice D), secured by {@code /api/v1/admin/**} →
 * {@code ROLE_ADMIN}. Thin controller over {@link AdminBrandService}.
 * <p>
 * List responses carry the {@code X-Total-Count} header and include inactive
 * brands; the body envelope is the standard
 * {@code {content, page, size, totalElements, totalPages}}.
 */
@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

    private final AdminBrandService adminBrandService;

    @GetMapping
    public PageResponse<AdminBrandDTO> getBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletResponse response) {
        PageResponse<AdminBrandDTO> result = adminBrandService.getBrands(page, size);
        response.setHeader("X-Total-Count", String.valueOf(result.totalElements()));
        return result;
    }

    @GetMapping("/{id}")
    public AdminBrandDTO getBrand(@PathVariable Long id) {
        return adminBrandService.getBrand(id);
    }

    @PostMapping
    public ResponseEntity<AdminBrandDTO> createBrand(
            @Valid @RequestBody AdminBrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminBrandService.createBrand(request));
    }

    @PutMapping("/{id}")
    public AdminBrandDTO updateBrand(@PathVariable Long id,
                                     @Valid @RequestBody AdminBrandRequest request) {
        return adminBrandService.updateBrand(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        adminBrandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}
