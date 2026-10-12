package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminProductService;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminProductDetailDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminProductRequest;
import com.fashion.ecommerce.presentation.dto.admin.AdminProductSummaryDTO;
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
 * Admin product catalog endpoints (slice B), secured by
 * {@code /api/v1/admin/**} → {@code ROLE_ADMIN}.
 * <p>
 * List responses carry the {@code X-Total-Count} header (REQ-API-003) exactly
 * like the storefront catalog; the body envelope is the standard
 * {@code {content, page, size, totalElements, totalPages}}.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public PageResponse<AdminProductSummaryDTO> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletResponse response) {
        PageResponse<AdminProductSummaryDTO> result = adminProductService
                .getProducts(search, status, categoryId, brandId, page, size);
        response.setHeader("X-Total-Count", String.valueOf(result.totalElements()));
        return result;
    }

    @GetMapping("/{id}")
    public AdminProductDetailDTO getProduct(@PathVariable Long id) {
        return adminProductService.getProduct(id);
    }

    @PostMapping
    public ResponseEntity<AdminProductDetailDTO> createProduct(
            @Valid @RequestBody AdminProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminProductService.createProduct(request));
    }

    @PutMapping("/{id}")
    public AdminProductDetailDTO updateProduct(@PathVariable Long id,
                                               @Valid @RequestBody AdminProductRequest request) {
        return adminProductService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        adminProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public AdminProductDetailDTO publishProduct(@PathVariable Long id) {
        return adminProductService.publishProduct(id);
    }

    @PostMapping("/{id}/unpublish")
    public AdminProductDetailDTO unpublishProduct(@PathVariable Long id) {
        return adminProductService.unpublishProduct(id);
    }
}
