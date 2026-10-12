package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminImageService;
import com.fashion.ecommerce.presentation.dto.admin.AdminImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin image endpoints (slice C), secured by {@code /api/v1/admin/**} →
 * {@code ROLE_ADMIN}. Thin controller over {@link AdminImageService}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminImageController {

    private final AdminImageService adminImageService;

    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminImageDTO> uploadImage(@PathVariable Long productId,
                                                     @RequestPart("file") MultipartFile file,
                                                     @RequestParam(required = false) String altText,
                                                     @RequestParam(required = false) Integer position,
                                                     @RequestParam(required = false) Boolean isPrimary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminImageService.uploadImage(productId, file, altText, position, isPrimary));
    }

    @PostMapping("/images/{id}/primary")
    public AdminImageDTO setPrimary(@PathVariable Long id) {
        return adminImageService.setPrimary(id);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        adminImageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
