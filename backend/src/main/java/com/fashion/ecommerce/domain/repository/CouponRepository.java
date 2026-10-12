package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    Optional<Coupon> findByCode(String code);
    
    boolean existsByCode(String code);
    
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true AND c.startsAt <= :now AND c.expiresAt >= :now")
    Optional<Coupon> findValidCouponByCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("""
        SELECT c FROM Coupon c 
        WHERE c.isActive = true 
        AND c.startsAt <= :now 
        AND c.expiresAt >= :now
        AND (c.usageLimit IS NULL OR c.usageCount < c.usageLimit)
        ORDER BY c.createdAt DESC
        """)
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);
}
