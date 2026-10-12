package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    
    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    @EntityGraph(attributePaths = {"size", "color", "inventories"})
    List<ProductVariant> findByProductIdIn(Collection<Long> productIds);
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.size.id = :sizeId AND pv.color.id = :colorId AND pv.isActive = true")
    Optional<ProductVariant> findByProductAndSizeAndColor(
        @Param("productId") Long productId,
        @Param("sizeId") Long sizeId,
        @Param("colorId") Long colorId
    );
    
    @Query("""
        SELECT COALESCE(SUM(i.quantityOnHand - i.quantityReserved), 0)
        FROM Inventory i
        WHERE i.variant.id = :variantId
        """)
    Integer getTotalAvailableStock(@Param("variantId") Long variantId);
    
    boolean existsBySku(String sku);
}
