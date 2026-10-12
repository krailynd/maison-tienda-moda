package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminVariantService;
import com.fashion.ecommerce.presentation.dto.admin.AdminVariantDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminVariantRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin variant endpoints (slice C), secured by {@code /api/v1/admin/**} →
 * {@code ROLE_ADMIN}. Thin controller over {@link AdminVariantService}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminVariantController {

    private final AdminVariantService adminVariantService;

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<AdminVariantDTO> createVariant(@PathVariable Long productId,
                                                         @Valid @RequestBody AdminVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminVariantService.createVariant(productId, request));
    }

    @PutMapping("/variants/{variantId}")
    public AdminVariantDTO updateVariant(@PathVariable Long variantId,
                                         @Valid @RequestBody AdminVariantRequest request) {
        return adminVariantService.updateVariant(variantId, request);
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long variantId) {
        adminVariantService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
