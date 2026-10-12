package com.fashion.ecommerce.domain.repository;

import com.fashion.ecommerce.domain.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByVariantId(Long variantId);

    Optional<Inventory> findByVariantIdAndWarehouseId(Long variantId, Long warehouseId);

    void deleteByVariantId(Long variantId);
}
