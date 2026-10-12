package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Brand;
import com.fashion.ecommerce.domain.model.Category;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fashion.ecommerce.domain.repository.BrandRepository;
import com.fashion.ecommerce.domain.repository.CategoryRepository;
import com.fashion.ecommerce.domain.repository.ProductRepository;
import com.fashion.ecommerce.domain.repository.ProductVariantRepository;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminProductDetailDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminProductRequest;
import com.fashion.ecommerce.presentation.dto.admin.AdminProductSummaryDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin write/read side of the product catalog (slice B).
 * <p>
 * Unlike {@link CatalogService} (storefront), this service sees the WHOLE
 * catalog: drafts ({@code is_active = false}) and inactive products are listed,
 * editable and deletable. New products are ALWAYS created as drafts regardless
 * of any payload (CAT-4); visibility is toggled with publish/unpublish.
 * <p>
 * Error contract: missing products → 404 (ResponseStatusException), duplicate
 * slugs → 409 (IllegalStateException, mapped by the global handler), unknown
 * category/brand references → 400 (IllegalArgumentException).
 */
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("18.00");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminProductSummaryDTO> getProducts(String search, String status,
                                                            Long categoryId, Long brandId,
                                                            int page, int size) {
        Specification<Product> spec = buildSpecification(search, status, categoryId, brandId);
        Pageable pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE), Sort.by("createdAt").descending());
        Page<Product> result = productRepository.findAll(spec, pageable);
        return PageResponse.of(result, toSummaryList(result.getContent()));
    }

    @Transactional(readOnly = true)
    public AdminProductDetailDTO getProduct(Long id) {
        return AdminProductDetailDTO.from(findProduct(id));
    }

    @Transactional
    public AdminProductDetailDTO createProduct(AdminProductRequest request) {
        validateSlugAvailable(request.slug(), null);
        Product product = Product.builder().build();
        applyRequest(product, request);
        // CAT-4: products always start as drafts; publish is an explicit action.
        product.setIsActive(false);
        return AdminProductDetailDTO.from(productRepository.save(product));
    }

    @Transactional
    public AdminProductDetailDTO updateProduct(Long id, AdminProductRequest request) {
        Product product = findProduct(id);
        validateSlugAvailable(request.slug(), id);
        applyRequest(product, request);
        // Updating never flips visibility — publish/unpublish own that flag.
        return AdminProductDetailDTO.from(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        // @SQLDelete on Product turns this into a soft delete (deleted_at).
        productRepository.delete(findProduct(id));
    }

    @Transactional
    public AdminProductDetailDTO publishProduct(Long id) {
        Product product = findProduct(id);
        product.setIsActive(true);
        return AdminProductDetailDTO.from(product);
    }

    @Transactional
    public AdminProductDetailDTO unpublishProduct(Long id) {
        Product product = findProduct(id);
        product.setIsActive(false);
        return AdminProductDetailDTO.from(product);
    }

    // ---------- internals ----------

    private Product findProduct(Long id) {
        return productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + id));
    }

    /**
     * A slug that already belongs to another (non-deleted) product is a
     * duplicate → 409 via {@link IllegalStateException}.
     */
    private void validateSlugAvailable(String slug, Long excludeId) {
        boolean taken = excludeId != null
                ? productRepository.existsBySlugAndIdNot(slug, excludeId)
                : productRepository.existsBySlug(slug);
        if (taken) {
            throw new IllegalStateException("Slug already in use: " + slug);
        }
    }

    private void applyRequest(Product p, AdminProductRequest r) {
        p.setName(r.name());
        p.setSlug(r.slug());
        p.setDescription(r.description());
        p.setShortDescription(r.shortDescription());
        p.setSkuPrefix(r.skuPrefix());
        p.setBrand(resolveBrand(r.brandId()));
        p.setCategory(resolveCategory(r.categoryId()));
        p.setPrice(r.price());
        p.setComparePrice(r.comparePrice());
        p.setCostPrice(r.costPrice());
        p.setTaxRate(r.taxRate() != null ? r.taxRate() : DEFAULT_TAX_RATE);
        p.setWeight(r.weight());
        p.setLengthCm(r.lengthCm());
        p.setWidthCm(r.widthCm());
        p.setHeightCm(r.heightCm());
        p.setIsFeatured(Boolean.TRUE.equals(r.isFeatured()));
        p.setIsNew(Boolean.TRUE.equals(r.isNew()));
        p.setIsEcoFriendly(Boolean.TRUE.equals(r.isEcoFriendly()));
        p.setStockTracking(r.stockTracking() != null ? r.stockTracking() : true);
        p.setLowStockThreshold(r.lowStockThreshold() != null ? r.lowStockThreshold() : 5);
        p.setMinOrderQuantity(r.minOrderQuantity() != null ? r.minOrderQuantity() : 1);
        p.setMaxOrderQuantity(r.maxOrderQuantity() != null ? r.maxOrderQuantity() : 99);
        p.setSeoTitle(r.seoTitle());
        p.setSeoDescription(r.seoDescription());
        p.setMetaKeywords(r.metaKeywords());
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + brandId));
    }

    private Specification<Product> buildSpecification(String search, String status,
                                                      Long categoryId, Long brandId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // NOTE: no isActive predicate here — the admin list includes drafts
            // and inactive products by design.
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), like));
            }
            if (StringUtils.hasText(status)) {
                if ("active".equalsIgnoreCase(status)) {
                    predicates.add(cb.isTrue(root.get("isActive")));
                } else if ("inactive".equalsIgnoreCase(status)) {
                    predicates.add(cb.isFalse(root.get("isActive")));
                }
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Batched variant lookup per product id so the admin list stays N+1-free on
     * the variant fetch (inventory totals are lazy but page-sized).
     */
    private List<AdminProductSummaryDTO> toSummaryList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductVariant>> variantsByProduct = variantRepository
                .findByProductIdIn(ids).stream()
                .collect(Collectors.groupingBy(pv -> pv.getProduct().getId()));
        return products.stream()
                .map(p -> AdminProductSummaryDTO.from(
                        p, variantsByProduct.getOrDefault(p.getId(), List.of())))
                .toList();
    }
}
