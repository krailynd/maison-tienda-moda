package com.fashion.ecommerce.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventories")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand = 0;

    @Column(name = "quantity_reserved")
    private Integer quantityReserved = 0;

    @Column(name = "reorder_point")
    private Integer reorderPoint = 0;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public Integer getQuantityAvailable() {
        if (quantityOnHand == null) return 0;
        if (quantityReserved == null) return quantityOnHand;
        return quantityOnHand - quantityReserved;
    }

    public boolean isLowStock() {
        return getQuantityAvailable() <= reorderPoint;
    }

    public boolean isOutOfStock() {
        return getQuantityAvailable() <= 0;
    }

    public void reserveStock(Integer quantity) {
        if (quantityReserved == null) quantityReserved = 0;
        quantityReserved += quantity;
    }

    public void releaseReservedStock(Integer quantity) {
        if (quantityReserved == null || quantityReserved < quantity) {
            throw new IllegalStateException("Not enough reserved stock");
        }
        quantityReserved -= quantity;
    }

    public void addStock(Integer quantity) {
        if (quantityOnHand == null) quantityOnHand = 0;
        quantityOnHand += quantity;
    }

    public void removeStock(Integer quantity) {
        if (quantityOnHand == null || quantityOnHand < quantity) {
            throw new IllegalStateException("Not enough stock");
        }
        quantityOnHand -= quantity;
    }
}
