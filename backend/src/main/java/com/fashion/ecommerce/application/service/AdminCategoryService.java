package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Category;
import com.fashion.ecommerce.domain.repository.CategoryRepository;
import com.fashion.ecommerce.domain.repository.ProductRepository;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminCategoryDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminCategoryRequest;
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
 * Admin write/read side of the category tree (slice D).
 * <p>
 * Unlike {@link CatalogService} (storefront), the admin list sees the WHOLE
 * tree — inactive categories are listed, editable and deletable. Category
 * rows are soft-deleted via {@code @SQLDelete} on {@link Category}.
 * <p>
 * Error contract mirrors {@link AdminProductService}: missing category → 404,
 * duplicate slug → 409 ({@link IllegalStateException}), unknown parent → 400
 * ({@link IllegalArgumentException}). Deleting a category that still has
 * children or products is refused with 409 CONFLICT (CAT-3 guard).
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminCategoryDTO> getCategories(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()));
        Page<Category> result = categoryRepository.findAll(pageable);
        return PageResponse.of(result, result.getContent().stream()
                .map(AdminCategoryDTO::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public AdminCategoryDTO getCategory(Long id) {
        return AdminCategoryDTO.from(findCategory(id));
    }

    @Transactional
    public AdminCategoryDTO createCategory(AdminCategoryRequest request) {
        validateSlugAvailable(request.slug(), null);
        Category category = Category.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .parent(resolveParent(request.parentId()))
                .imageUrl(request.imageUrl())
                .iconClass(request.iconClass())
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .build();
        category.setIsActive(request.isActive() == null || request.isActive());
        return AdminCategoryDTO.from(categoryRepository.save(category));
    }

    @Transactional
    public AdminCategoryDTO updateCategory(Long id, AdminCategoryRequest request) {
        Category category = findCategory(id);
        validateSlugAvailable(request.slug(), id);
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw new IllegalArgumentException("Category cannot be its own parent: " + id);
        }
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setParent(resolveParent(request.parentId()));
        category.setImageUrl(request.imageUrl());
        category.setIconClass(request.iconClass());
        category.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        category.setSeoTitle(request.seoTitle());
        category.setSeoDescription(request.seoDescription());
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }
        return AdminCategoryDTO.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        // CAT-3 guard: a category with children or products cannot be removed.
        if (categoryRepository.existsByParentId(id)) {
            throw new IllegalStateException("Cannot delete category with subcategories: " + id);
        }
        if (productRepository.countByCategory_Id(id) > 0) {
            throw new IllegalStateException("Cannot delete category with products: " + id);
        }
        // @SQLDelete on Category turns this into a soft delete (deleted_at).
        categoryRepository.delete(category);
    }

    // ---------- internals ----------

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found: " + id));
    }

    /**
     * A slug that already belongs to another (non-deleted) category is a
     * duplicate → 409 via {@link IllegalStateException}.
     */
    private void validateSlugAvailable(String slug, Long excludeId) {
        boolean taken = excludeId != null
                ? categoryRepository.existsBySlugAndIdNot(slug, excludeId)
                : categoryRepository.existsBySlug(slug);
        if (taken) {
            throw new IllegalStateException("Slug already in use: " + slug);
        }
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent category not found: " + parentId));
    }
}
