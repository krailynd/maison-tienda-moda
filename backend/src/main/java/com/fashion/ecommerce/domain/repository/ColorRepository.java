package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorRepository extends JpaRepository<Color, Long> {
    
    Optional<Color> findByHexCode(String hexCode);

    List<Color> findAllByOrderByDisplayOrderAsc();

    boolean existsByHexCode(String hexCode);
}
