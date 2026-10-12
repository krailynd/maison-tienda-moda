package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Wishlist;
import com.fashion.ecommerce.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    Optional<Wishlist> findByUser(User user);
    
    Optional<Wishlist> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
