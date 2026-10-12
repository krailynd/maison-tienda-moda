package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Brand;
import com.fashion.ecommerce.domain.model.Category;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductImage;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fashion.ecommerce.domain.repository.BrandRepository;
import com.fashion.ecommerce.domain.repository.CategoryRepository;
import com.fashion.ecommerce.domain.repository.ProductImageRepository;
import com.fashion.ecommerce.domain.repository.ProductRepository;
import com.fashion.ecommerce.domain.repository.ProductVariantRepository;
import com.fashion.ecommerce.presentation.dto.BrandDTO;
import com.fashion.ecommerce.presentation.dto.CategoryDTO;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.ProductDetailDTO;
import com.fashion.ecommerce.presentation.dto.ProductSummaryDTO;
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

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read side of the public catalog: products, categories, brands and related
 * products. Every DTO is mapped here inside a read-only transaction so no lazy
 * association is touched outside a session.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDTO> getProducts(String categorySlug, String search,
                                                       String sort, int page, int size) {
        Set<Long> categoryIds = resolveCategoryIds(categorySlug);
        Specification<Product> spec = buildSpecification(categoryIds, search);
        Pageable pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE), toSort(sort));
        Page<Product> result = productRepository.findAll(spec, pageable);
        return PageResponse.of(result, toSummaryList(result.getContent()));
    }

    @Transactional(readOnly = true)
    public ProductDetailDTO getProduct(String slugOrId) {
        Optional<Product> found = productRepository.findWithDetailsBySlug(slugOrId);
        if (found.isEmpty() && slugOrId.matches("\\d+")) {
            found = productRepository.findWithDetailsById(Long.valueOf(slugOrId));
        }
        Product product = found.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + slugOrId));
        requirePublished(product, slugOrId);
        return ProductDetailDTO.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> getRelated(String slugOrId) {
        Product product = resolveProductRef(slugOrId);
        if (product.getCategory() == null) {
            return List.of();
        }
        List<Product> related = productRepository.findRelated(
                product.getCategory().getId(), product.getId(), PageRequest.of(0, 4));
        return toSummaryList(related);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategories() {
        List<Category> categories = categoryRepository.findAllActiveOrdered();
        Map<Long, Long> counts = categoryRepository.countProductsByCategory().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()));

        Map<Long, List<Category>> byParent = categories.stream()
                .collect(Collectors.groupingBy(c ->
                        c.getParent() != null ? c.getParent().getId() : -1L, Collectors.toList()));

        return categories.stream()
                .filter(c -> c.getParent() == null)
                .sorted(Comparator.comparing(Category::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(c -> CategoryDTO.from(c, counts, byParent))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BrandDTO> getBrands() {
        return brandRepository.findByIsActiveTrueOrderByDisplayOrder().stream()
                .map(BrandDTO::from)
                .toList();
    }

    private Product resolveProductRef(String slugOrId) {
        Optional<Product> found = productRepository.findBySlug(slugOrId);
        if (found.isEmpty() && slugOrId.matches("\\d+")) {
            found = productRepository.findById(Long.valueOf(slugOrId));
        }
        Product product = found.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + slugOrId));
        requirePublished(product, slugOrId);
        return product;
    }

    /**
     * CAT-6 gap fix: a draft ({@code is_active = false}) or otherwise
     * unpublished product must never be served by the public catalog. The same
     * "not found" message is used so the response does not leak that a draft
     * exists.
     */
    private void requirePublished(Product product, String slugOrId) {
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + slugOrId);
        }
    }

    private Set<Long> resolveCategoryIds(String categorySlug) {
        if (!StringUtils.hasText(categorySlug)) {
            return null;
        }
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));
        return categoryRepository.findCategoryWithChildren(category.getId()).stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
    }

    private Specification<Product> buildSpecification(Set<Long> categoryIds, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), like));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort toSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "price_asc", "precio-asc" -> Sort.by("price").ascending();
            case "price_desc", "precio-desc" -> Sort.by("price").descending();
            case "popularity", "relevancia" -> Sort.by("purchaseCount").descending();
            default -> Sort.by("createdAt").descending(); // newest
        };
    }

    /**
     * Maps a list of products to card DTOs with batched lookups for images,
     * variants, categories and brands so the mapping stays N+1-free.
     */
    private List<ProductSummaryDTO> toSummaryList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<Long> ids = products.stream().map(Product::getId).toList();

        Map<Long, List<ProductImage>> imagesByProduct = imageRepository
                .findByProductIdInOrderByPositionAsc(ids).stream()
                .collect(Collectors.groupingBy(im -> im.getProduct().getId()));

        Map<Long, List<ProductVariant>> variantsByProduct = variantRepository
                .findByProductIdIn(ids).stream()
                .collect(Collectors.groupingBy(pv -> pv.getProduct().getId()));

        Map<Long, Category> categoriesById = batchById(
                products.stream()
                        .map(Product::getCategory)
                        .filter(java.util.Objects::nonNull)
                        .map(Category::getId)
                        .distinct()
                        .toList(),
                categoryRepository::findAllById,
                Category::getId);

        Map<Long, Brand> brandsById = batchById(
                products.stream()
                        .map(Product::getBrand)
                        .filter(java.util.Objects::nonNull)
                        .map(Brand::getId)
                        .distinct()
                        .toList(),
                brandRepository::findAllById,
                Brand::getId);

        return products.stream()
                .map(p -> ProductSummaryDTO.from(
                        p,
                        imagesByProduct.getOrDefault(p.getId(), List.of()),
                        variantsByProduct.getOrDefault(p.getId(), List.of()),
                        p.getCategory() != null ? categoriesById.get(p.getCategory().getId()) : null,
                        p.getBrand() != null ? brandsById.get(p.getBrand().getId()) : null))
                .toList();
    }

    private <T, ID> Map<ID, T> batchById(List<ID> ids, Function<List<ID>, List<T>> loader,
                                         Function<T, ID> idExtractor) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return loader.apply(ids).stream()
                .collect(Collectors.toMap(idExtractor, Function.identity()));
    }
}
