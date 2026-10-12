package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Page<Review> findByProductId(Long productId, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Page<Review> findApprovedReviewsByProduct(@Param("productId") Long productId, Pageable pageable);
    
    List<Review> findByUserIdAndIsApprovedTrue(Long userId);
    
    @Query("""
        SELECT COALESCE(AVG(r.rating), 0) 
        FROM Review r 
        WHERE r.product.id = :productId AND r.isApproved = true
        """)
    Double getAverageRatingByProduct(@Param("productId") Long productId);
    
    @Query("""
        SELECT COUNT(r) FROM Review r
        WHERE r.product.id = :productId AND r.isApproved = true
        """)
    long countApprovedReviewsByProduct(@Param("productId") Long productId);
    
    @Query("""
        SELECT r.rating, COUNT(r) 
        FROM Review r 
        WHERE r.product.id = :productId AND r.isApproved = true
        GROUP BY r.rating
        ORDER BY r.rating DESC
        """)
    List<Object[]> getRatingDistributionByProduct(@Param("productId") Long productId);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
