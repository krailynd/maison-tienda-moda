package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only access to {@link Season} for the admin collection editor dropdown
 * (slice D). Seasons are seeded by V1 (PV2026 / OI2026) and have no admin CRUD.
 */
@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {

    List<Season> findAllByIsActiveTrueOrderByStartDateAsc();
}
