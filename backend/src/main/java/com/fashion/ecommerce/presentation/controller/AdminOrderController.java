package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.AdminOrderService;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminOrderDetailDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminOrderSummaryDTO;
import com.fashion.ecommerce.presentation.dto.admin.OrderStatusUpdateRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Admin order endpoints (slice E), secured by {@code /api/v1/admin/**} →
 * {@code ROLE_ADMIN}.
 * <p>
 * List responses carry the {@code X-Total-Count} header (REQ-API-003) exactly
 * like the other admin lists; the body envelope is the standard
 * {@code {content, page, size, totalElements, totalPages}}. Transition errors
 * follow the envelope: 400 unknown status, 404 missing order, 409 illegal
 * transition.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public PageResponse<AdminOrderSummaryDTO> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletResponse response) {
        PageResponse<AdminOrderSummaryDTO> result = adminOrderService
                .getOrders(status, paymentStatus, parseDate(dateFrom), parseDate(dateTo),
                        search, page, size);
        response.setHeader("X-Total-Count", String.valueOf(result.totalElements()));
        return result;
    }

    @GetMapping("/{id}")
    public AdminOrderDetailDTO getOrder(@PathVariable Long id) {
        return adminOrderService.getOrder(id);
    }

    @PostMapping("/{id}/status")
    public AdminOrderDetailDTO updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return adminOrderService.transitionOrder(id, request);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date (expected yyyy-MM-dd): " + value);
        }
    }
}
