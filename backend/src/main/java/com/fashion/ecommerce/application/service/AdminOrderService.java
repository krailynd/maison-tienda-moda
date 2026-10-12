package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Order;
import com.fashion.ecommerce.domain.model.OrderItem;
import com.fashion.ecommerce.domain.repository.OrderRepository;
import com.fashion.ecommerce.presentation.dto.PageResponse;
import com.fashion.ecommerce.presentation.dto.admin.AdminOrderDetailDTO;
import com.fashion.ecommerce.presentation.dto.admin.AdminOrderSummaryDTO;
import com.fashion.ecommerce.presentation.dto.admin.OrderStatusUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin read/write side of orders (slice E).
 * <p>
 * List filters on status / paymentStatus / date range / search
 * (orderNumber or customer email, including guest orders whose email lives in
 * the JSONB address snapshot). Detail and transitions work on lazy-loaded
 * collections inside {@code @Transactional} methods: the {@code orders} table
 * is RANGE-partitioned by {@code order_year} with a composite PK, and fetching
 * multiple collections eagerly in one query raises MultipleBagFetchException,
 * so no join fetches are used.
 * <p>
 * Error contract: missing order → 404 (ResponseStatusException), unknown
 * status name → 400 (IllegalArgumentException), illegal transition → 409
 * (IllegalStateException, status unchanged).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryDTO> getOrders(String status, String paymentStatus,
                                                        LocalDate dateFrom, LocalDate dateTo,
                                                        String search, int page, int size) {
        Specification<Order> spec = buildSpecification(status, paymentStatus, dateFrom, dateTo, search);
        Pageable pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE), Sort.by("createdAt").descending());
        Page<Order> result = orderRepository.findAll(spec, pageable);
        return PageResponse.of(result, toSummaryList(result.getContent()));
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailDTO getOrder(Long id) {
        return AdminOrderDetailDTO.from(findOrder(id), objectMapper);
    }

    /**
     * Validates the requested transition against the domain matrix BEFORE any
     * mutation: illegal transitions throw and leave the order unchanged.
     * Legal transitions are recorded through {@link Order#addStatusHistory}
     * (which also stamps shippedAt/deliveredAt) plus the CANCELLED /
     * REFUNDED side effects.
     */
    @Transactional
    public AdminOrderDetailDTO transitionOrder(Long id, OrderStatusUpdateRequest request) {
        Order order = findOrder(id);
        Order.OrderStatus target = parseStatus(request.newStatus());
        Order.OrderStatus previous = order.getStatus();

        if (!order.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Illegal order status transition from " + previous + " to " + target);
        }

        order.addStatusHistory(target, request.notes());

        if (target == Order.OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
            order.setCancellationReason(request.notes());
        } else if (target == Order.OrderStatus.REFUNDED && request.refundAmount() != null) {
            order.setRefundAmount(request.refundAmount());
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} status {} -> {} (admin)", saved.getOrderNumber(), previous, target);
        return AdminOrderDetailDTO.from(saved, objectMapper);
    }

    // ---------- internals ----------

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + id));
    }

    private Specification<Order> buildSpecification(String status, String paymentStatus,
                                                    LocalDate dateFrom, LocalDate dateTo,
                                                    String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            if (StringUtils.hasText(paymentStatus)) {
                predicates.add(cb.equal(root.get("paymentStatus"), parsePaymentStatus(paymentStatus)));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(LocalTime.MAX)));
            }
            if (StringUtils.hasText(search)) {
                // Guest orders have no user row: their email only exists inside
                // the JSONB address snapshot, so the JSON columns are matched
                // via a dedicated JPQL query (JPQL CAST), and its ids are ANDed
                // here with the remaining filters.
                List<Order> matches = orderRepository.searchByKeyword("%" + search.trim().toLowerCase() + "%");
                if (matches.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    List<Long> ids = matches.stream().map(Order::getId).toList();
                    predicates.add(root.get("id").in(ids));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Order.OrderStatus parseStatus(String status) {
        try {
            return Order.OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown order status: " + status);
        }
    }

    private Order.PaymentStatus parsePaymentStatus(String paymentStatus) {
        try {
            return Order.PaymentStatus.valueOf(paymentStatus.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown payment status: " + paymentStatus);
        }
    }

    /**
     * Batched item lookup per order id so the admin list stays N+1-free on the
     * item-count column (the items themselves are lazy but page-sized).
     */
    private List<AdminOrderSummaryDTO> toSummaryList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Long> ids = orders.stream().map(Order::getId).toList();
        Map<Long, Long> countsByOrder = orderRepository.findItemsByOrderIds(ids).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId(), Collectors.counting()));
        return orders.stream()
                .map(o -> AdminOrderSummaryDTO.from(o,
                        countsByOrder.getOrDefault(o.getId(), 0L).intValue(), objectMapper))
                .toList();
    }
}
