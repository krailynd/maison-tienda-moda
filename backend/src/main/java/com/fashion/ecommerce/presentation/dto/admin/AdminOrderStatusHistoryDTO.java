package com.fashion.ecommerce.presentation.dto.admin;

import com.fashion.ecommerce.domain.model.OrderStatusHistory;

import java.time.LocalDateTime;

/**
 * One entry of the order status timeline, served chronologically (oldest
 * first) on the admin order detail.
 */
public record AdminOrderStatusHistoryDTO(
        Long id,
        String oldStatus,
        String newStatus,
        String reason,
        LocalDateTime changedAt) {

    public static AdminOrderStatusHistoryDTO from(OrderStatusHistory history) {
        return new AdminOrderStatusHistoryDTO(
                history.getId(),
                history.getOldStatus() != null ? history.getOldStatus().name() : null,
                history.getNewStatus().name(),
                history.getReason(),
                history.getChangedAt());
    }
}
