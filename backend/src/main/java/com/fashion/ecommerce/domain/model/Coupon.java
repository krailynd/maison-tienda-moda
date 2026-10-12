package com.fashion.ecommerce.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_purchase_amount", precision = 10, scale = 2)
    private BigDecimal minPurchaseAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(name = "times_used")
    private Integer usageCount = 0;

    @Array(length = 50)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applicable_categories")
    @Builder.Default
    private Long[] applicableCategories = new Long[0];

    @Array(length = 50)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_categories")
    @Builder.Default
    private Long[] excludedCategories = new Long[0];

    @Array(length = 50)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applicable_products")
    @Builder.Default
    private Long[] applicableProducts = new Long[0];

    @Array(length = 50)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_products")
    @Builder.Default
    private Long[] excludedProducts = new Long[0];

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "valid_until")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "auto_apply")
    private Boolean autoApply = false;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isValid() {
        if (!Boolean.TRUE.equals(isActive)) return false;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startsAt) || now.isAfter(expiresAt)) return false;

        if (usageLimit != null && usageCount >= usageLimit) return false;

        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (!isValid()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = switch (discountType) {
            case PERCENTAGE -> subtotal.multiply(discountValue.divide(new BigDecimal("100")));
            case FIXED_AMOUNT -> discountValue;
            case BUY_X_GET_Y -> subtotal; // Special logic required
            case FREE_SHIPPING -> BigDecimal.ZERO; // Discount applied to shipping, not subtotal
        };

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            return maxDiscountAmount;
        }

        return discount;
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT,
        BUY_X_GET_Y,
        FREE_SHIPPING
    }
}
