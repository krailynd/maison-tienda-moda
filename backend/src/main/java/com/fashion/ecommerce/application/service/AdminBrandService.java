package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Brand;
import com.fashion.ecommerce.domain.repository.BrandRepository;
import com.fashion.ecommerce.domain.repository.ProductRepository;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminBrandDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminBrandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin write/read side of brands (slice D).
 * <p>
 * The admin list sees ALL brands regardless of activation state; rows are
 * soft-deleted via {@code @SQLDelete} on {@link Brand}.
 * <p>
 * Error contract mirrors {@link AdminProductService}: missing brand → 404,
 * duplicate slug → 409 ({@link IllegalStateException}). Deleting a brand that
 * is still referenced by products is refused with 409 CONFLICT (same referential
 * guard as categories).
 */
@Service
@RequiredArgsConstructor
public class AdminBrandService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminBrandDTO> getBrands(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()));
        Page<Brand> result = brandRepository.findAll(pageable);
        return PageResponse.of(result, result.getContent().stream()
                .map(AdminBrandDTO::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public AdminBrandDTO getBrand(Long id) {
        return AdminBrandDTO.from(findBrand(id));
    }

    @Transactional
    public AdminBrandDTO createBrand(AdminBrandRequest request) {
        validateSlugAvailable(request.slug(), null);
        Brand brand = Brand.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .logoUrl(request.logoUrl())
                .websiteUrl(request.websiteUrl())
                .isFeatured(Boolean.TRUE.equals(request.isFeatured()))
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();
        brand.setIsActive(request.isActive() == null || request.isActive());
        return AdminBrandDTO.from(brandRepository.save(brand));
    }

    @Transactional
    public AdminBrandDTO updateBrand(Long id, AdminBrandRequest request) {
        Brand brand = findBrand(id);
        validateSlugAvailable(request.slug(), id);
        brand.setName(request.name());
        brand.setSlug(request.slug());
        brand.setDescription(request.description());
        brand.setLogoUrl(request.logoUrl());
        brand.setWebsiteUrl(request.websiteUrl());
        brand.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        if (request.isFeatured() != null) {
            brand.setIsFeatured(request.isFeatured());
        }
        if (request.isActive() != null) {
            brand.setIsActive(request.isActive());
        }
        return AdminBrandDTO.from(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findBrand(id);
        // Referential guard: a brand referenced by products cannot be removed.
        if (productRepository.countByBrand_Id(id) > 0) {
            throw new IllegalStateException("Cannot delete brand with products: " + id);
        }
        // @SQLDelete on Brand turns this into a soft delete (deleted_at).
        brandRepository.delete(brand);
    }

    // ---------- internals ----------

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Brand not found: " + id));
    }

    /**
     * A slug that already belongs to another (non-deleted) brand is a
     * duplicate → 409 via {@link IllegalStateException}.
     */
    private void validateSlugAvailable(String slug, Long excludeId) {
        boolean taken = excludeId != null
                ? brandRepository.existsBySlugAndIdNot(slug, excludeId)
                : brandRepository.existsBySlug(slug);
        if (taken) {
            throw new IllegalStateException("Slug already in use: " + slug);
        }
    }
}
