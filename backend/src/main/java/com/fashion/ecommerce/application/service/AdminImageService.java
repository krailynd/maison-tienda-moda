package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.application.exception.ValidationException;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductImage;
import com.fashion.ecommerce.domain.repository.ProductImageRepository;
import com.fashion.ecommerce.domain.repository.ProductRepository;
import com.fashion.ecommerce.presentation.dto.admin.AdminImageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin write side of product images (slice C, design D4).
 * <p>
 * Files are stored on the local host under {@code app.uploads.dir} with
 * server-generated {@code UUID + sanitized extension} names and served back at
 * {@code /uploads/{filename}} (see {@link
 * com.fashion.ecommerce.infrastructure.config.WebConfig}). Content-type
 * whitelist and a 10MB size guard run BEFORE anything is persisted; a product
 * keeps exactly ONE primary image (demotion on promotion, promotion on primary
 * deletion). Thumbnails are deferred — {@code thumbnailUrl} stays null.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminImageService {

    /** Mirrors {@code spring.servlet.multipart.max-file-size} in the config. */
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;

    @Value("${app.uploads.dir:./uploads}")
    private String uploadsDir;

    @Transactional
    public AdminImageDTO uploadImage(Long productId, MultipartFile file,
                                     String altText, Integer position, Boolean isPrimary) {
        Product product = findProduct(productId);

        if (file == null || file.isEmpty()) {
            throw new ValidationException("A file is required", Map.of("file", "No file was uploaded"));
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ValidationException("Unsupported file type. Allowed: JPEG, PNG, WEBP, GIF",
                    Map.of("file", "Unsupported content type: " + file.getContentType()));
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            // Same contract as the container-level multipart guard (slice A):
            // 400 PAYLOAD_TOO_LARGE, nothing persisted.
            throw new MaxUploadSizeExceededException(MAX_FILE_BYTES);
        }

        String filename = UUID.randomUUID() + "." + extension;
        store(file, filename);

        List<ProductImage> existing = imageRepository.findByProductIdOrderByPositionAsc(productId);
        int nextPosition = existing.stream()
                .mapToInt(i -> i.getPosition() != null ? i.getPosition() : 0)
                .max()
                .orElse(0) + 1;
        boolean makePrimary = Boolean.TRUE.equals(isPrimary) || existing.isEmpty();

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl("/uploads/" + filename)
                .thumbnailUrl(null)
                .altText(altText)
                .position(position != null ? position : nextPosition)
                .isPrimary(makePrimary)
                .build();
        if (makePrimary) {
            demotePrimary(productId);
        }
        return AdminImageDTO.from(imageRepository.save(image));
    }

    @Transactional
    public AdminImageDTO setPrimary(Long imageId) {
        ProductImage image = findImage(imageId);
        demotePrimary(image.getProduct().getId());
        image.setIsPrimary(true);
        return AdminImageDTO.from(image);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        ProductImage image = findImage(imageId);
        Long productId = image.getProduct().getId();
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        deleteFile(image.getImageUrl());
        // @SQLDelete turns this into a soft delete (deleted_at).
        imageRepository.delete(image);

        // Keep exactly one primary per product: promote the first remaining
        // image so the storefront gallery never loses its hero URL.
        if (wasPrimary) {
            imageRepository.findByProductIdOrderByPositionAsc(productId).stream()
                    .findFirst()
                    .ifPresent(next -> next.setIsPrimary(true));
        }
    }

    // ---------- internals ----------

    private Product findProduct(Long productId) {
        return productRepository.findWithDetailsById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productId));
    }

    private ProductImage findImage(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Image not found: " + imageId));
    }

    private void store(MultipartFile file, String filename) {
        try {
            Path targetDir = Path.of(uploadsDir);
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(filename).toFile());
        } catch (IOException e) {
            log.error("Failed to store uploaded image {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to store the uploaded image");
        }
    }

    /**
     * Best-effort physical file removal. The DB row is already soft-deleted;
     * a leftover file on disk is harmless, so IO failures are swallowed.
     */
    private void deleteFile(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            Path base = Path.of(uploadsDir).toAbsolutePath().normalize();
            Path file = base.resolve(imageUrl.substring("/uploads/".length())).normalize();
            if (file.startsWith(base)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Could not delete file for image url {}", imageUrl, e);
        }
    }

    private void demotePrimary(Long productId) {
        imageRepository.demotePrimary(productId);
    }
}
