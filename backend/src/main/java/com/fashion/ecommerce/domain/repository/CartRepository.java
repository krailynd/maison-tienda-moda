package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Cart;
import com.fashion.ecommerce.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findBySessionId(String sessionId);

    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :threshold AND c.user IS NOT NULL")
    List<Cart> findAbandonedCarts(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT c FROM Cart c WHERE c.sessionId = :sessionId OR c.user.id = :userId")
    Optional<Cart> findGuestOrUserCart(@Param("sessionId") String sessionId, @Param("userId") Long userId);
    
    long countByUserId(Long userId);
}
