package com.fashion.ecommerce.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_year", nullable = false)
    private Integer orderYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private Order.OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private Order.OrderStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_by")
    private Long changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @PrePersist
    public void prePersist() {
        if (this.orderYear == null && this.order != null) {
            this.orderYear = this.order.getOrderYear();
        }
    }
}
