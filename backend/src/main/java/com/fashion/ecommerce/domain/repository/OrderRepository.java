package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Order;
import com.fashion.ecommerce.domain.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    
    Optional<Order> findByUuid(UUID uuid);

    // NOTE: no EntityGraph here: fetching both "items" and "payments" in one
    // query raises MultipleBagFetchException. Collections load lazily inside the
    // @Transactional checkout service methods.
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    /**
     * Batch item lookup for the admin order list: counts every order's lines in
     * one query instead of one lazy load per row (same N+1-avoidance pattern as
     * {@code AdminProductService}).
     */
    @Query("SELECT i FROM OrderItem i WHERE i.order.id IN :ids")
    List<OrderItem> findItemsByOrderIds(@Param("ids") Collection<Long> ids);

    /**
     * Keyword search for the admin list: matches the order number, the
     * registered customer email and — for guest orders — the email inside the
     * JSONB address snapshot (the JSON columns are cast to text; the JPA
     * Criteria API has no {@code cast} in jakarta.persistence 3.1, so this
     * lives in JPQL). The user join is EXPLICITLY left so guest orders
     * (user_id null) are not dropped by the implicit-join inner join. The
     * caller ANDs the returned ids with the remaining dynamic filters.
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.user u
            WHERE LOWER(o.orderNumber) LIKE LOWER(:search)
               OR LOWER(u.email) LIKE LOWER(:search)
               OR LOWER(CAST(o.billingAddress AS string)) LIKE LOWER(:search)
               OR LOWER(CAST(o.shippingAddress AS string)) LIKE LOWER(:search)
            """)
    List<Order> searchByKeyword(@Param("search") String like);
    
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Order.OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.user.id = :userId
        AND o.paymentStatus = 'PAID'
        AND o.deletedAt IS NULL
        """)
    Double getTotalSpentByUser(@Param("userId") Long userId);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.deletedAt IS NULL")
    List<Order> findOrdersBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("""
        SELECT DATE(o.createdAt) as date, COUNT(o) as orderCount, SUM(o.totalAmount) as revenue
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
        AND o.deletedAt IS NULL
        GROUP BY DATE(o.createdAt)
        ORDER BY date DESC
        """)
    List<Object[]> getDailySalesReport(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
