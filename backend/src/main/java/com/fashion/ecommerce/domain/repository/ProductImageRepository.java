package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdInOrderByPositionAsc(Collection<Long> productIds);

    List<ProductImage> findByProductIdOrderByPositionAsc(Long productId);

    /** Bulk demote keeps exactly ONE primary per product (enforced on promote). */
    @Modifying
    @Query("UPDATE ProductImage i SET i.isPrimary = false WHERE i.product.id = :productId")
    void demotePrimary(@Param("productId") Long productId);
}
