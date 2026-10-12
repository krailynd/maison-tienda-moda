package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SizeRepository extends JpaRepository<Size, Long> {
    
    Optional<Size> findByCode(String code);

    List<Size> findAllByOrderByDisplayOrderAsc();

    boolean existsByCode(String code);
}
