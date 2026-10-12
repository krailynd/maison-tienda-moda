package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence access for {@link Collection} (slice D). This is the only new
 * repository the admin-panel design required; rows are soft-deleted via
 * {@code @SQLDelete} on the entity.
 */
@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Optional<Collection> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
