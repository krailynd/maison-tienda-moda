package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Collection;
import com.fashion.ecommerce.domain.model.Season;
import com.fashion.ecommerce.domain.repository.CollectionRepository;
import com.fashion.ecommerce.domain.repository.SeasonRepository;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminCollectionDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminCollectionRequest;
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
 * Admin write/read side of collections (slice D).
 * <p>
 * Collections reference the seeded {@link Season} table (PV2026 / OI2026) and
 * are soft-deleted via {@code @SQLDelete} on {@link Collection}. The collection
 * table itself already exists in V1 — only {@link CollectionRepository} was
 * missing (zero DDL added).
 * <p>
 * Error contract mirrors {@link AdminProductService}: missing collection → 404,
 * duplicate slug → 409 ({@link IllegalStateException}), unknown season → 400
 * ({@link IllegalArgumentException}).
 */
@Service
@RequiredArgsConstructor
public class AdminCollectionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CollectionRepository collectionRepository;
    private final SeasonRepository seasonRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminCollectionDTO> getCollections(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by("name").ascending());
        Page<Collection> result = collectionRepository.findAll(pageable);
        return PageResponse.of(result, result.getContent().stream()
                .map(AdminCollectionDTO::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public AdminCollectionDTO getCollection(Long id) {
        return AdminCollectionDTO.from(findCollection(id));
    }

    @Transactional
    public AdminCollectionDTO createCollection(AdminCollectionRequest request) {
        validateSlugAvailable(request.slug(), null);
        Collection collection = Collection.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .bannerUrl(request.bannerUrl())
                .season(resolveSeason(request.seasonId()))
                .isFeatured(Boolean.TRUE.equals(request.isFeatured()))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();
        return AdminCollectionDTO.from(collectionRepository.save(collection));
    }

    @Transactional
    public AdminCollectionDTO updateCollection(Long id, AdminCollectionRequest request) {
        Collection collection = findCollection(id);
        validateSlugAvailable(request.slug(), id);
        collection.setName(request.name());
        collection.setSlug(request.slug());
        collection.setDescription(request.description());
        collection.setBannerUrl(request.bannerUrl());
        collection.setSeason(resolveSeason(request.seasonId()));
        if (request.isFeatured() != null) {
            collection.setIsFeatured(request.isFeatured());
        }
        collection.setStartDate(request.startDate());
        collection.setEndDate(request.endDate());
        return AdminCollectionDTO.from(collectionRepository.save(collection));
    }

    @Transactional
    public void deleteCollection(Long id) {
        // @SQLDelete on Collection turns this into a soft delete (deleted_at).
        collectionRepository.delete(findCollection(id));
    }

    // ---------- internals ----------

    private Collection findCollection(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Collection not found: " + id));
    }

    /**
     * A slug that already belongs to another (non-deleted) collection is a
     * duplicate → 409 via {@link IllegalStateException}.
     */
    private void validateSlugAvailable(String slug, Long excludeId) {
        boolean taken = excludeId != null
                ? collectionRepository.existsBySlugAndIdNot(slug, excludeId)
                : collectionRepository.existsBySlug(slug);
        if (taken) {
            throw new IllegalStateException("Slug already in use: " + slug);
        }
    }

    private Season resolveSeason(Long seasonId) {
        if (seasonId == null) {
            return null;
        }
        return seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Season not found: " + seasonId));
    }
}
