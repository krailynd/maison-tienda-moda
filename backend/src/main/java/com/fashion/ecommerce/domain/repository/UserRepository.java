package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUuid(UUID uuid);
    
    boolean existsByEmail(String email);
    
    long countByIsActiveTrue();
}
