package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // NOTE: only to-one associations are fetched here. Collections (variants,
    // variants.inventories, images) are loaded lazily inside the read-only
    // transaction of CatalogService.getProduct; fetching several of them in one
    // query would raise MultipleBagFetchException.
    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findWithDetailsBySlug(String slug);

    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findWithDetailsById(Long id);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id <> :productId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findRelated(@Param("categoryId") Long categoryId, @Param("productId") Long productId, Pageable pageable);
    
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    Page<Product> findByCategory_IdAndIsActiveTrue(Long categoryId, Pageable pageable);
    
    Page<Product> findByBrand_IdAndIsActiveTrue(Long brandId, Pageable pageable);
    
    Page<Product> findByIsNewTrueAndIsActiveTrue(Pageable pageable);
    
    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.comparePrice IS NOT NULL AND p.comparePrice > p.price")
    Page<Product> findOnSaleProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p JOIN p.category c WHERE c.slug IN :categorySlugs AND p.isActive = true")
    List<Product> findByCategorySlugs(@Param("categorySlugs") List<String> categorySlugs);
    
    @Query(value = """
        SELECT p.* FROM products p
        WHERE p.is_active = true
        AND p.deleted_at IS NULL
        AND (
            p.name ILIKE %:keyword% 
            OR p.description ILIKE %:keyword%
            OR p.meta_keywords ILIKE %:keyword%
        )
        ORDER BY ts_rank(to_tsvector('spanish', p.name || ' ' || COALESCE(p.description, '')), to_tsquery('spanish', :keyword)) DESC
        """, nativeQuery = true)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    long countByIsActiveTrue();
    
    long countByCategory_IdAndIsActiveTrue(Long categoryId);

    /**
     * Total products in a category regardless of activation state. {@code @Where}
     * on {@link Product} excludes soft-deleted rows — used by the admin category
     * delete guard.
     */
    long countByCategory_Id(Long categoryId);

    /**
     * Total products of a brand regardless of activation state (soft-deleted
     * rows excluded by {@code @Where}) — used by the admin brand delete guard.
     */
    long countByBrand_Id(Long brandId);
}
