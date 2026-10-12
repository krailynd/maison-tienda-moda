package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    
    Optional<Brand> findBySlug(String slug);
    
    List<Brand> findByIsActiveTrueOrderByDisplayOrder();
    
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
