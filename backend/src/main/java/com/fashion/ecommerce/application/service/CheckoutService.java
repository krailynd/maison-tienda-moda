package com.fashion.ecommerce.application.service;

import com.fashion.ecommerce.domain.model.Order;
import com.fashion.ecommerce.domain.model.OrderItem;
import com.fashion.ecommerce.domain.model.Payment;
import com.fashion.ecommerce.domain.model.Product;
import com.fashion.ecommerce.domain.model.ProductVariant;
import com.fashion.ecommerce.domain.repository.OrderRepository;
import com.fashion.ecommerce.domain.repository.ProductVariantRepository;
import com.fashion.ecommerce.presentation.dto.CreateOrderRequest;
import com.fashion.ecommerce.presentation.dto.CustomerRequest;
import com.fashion.ecommerce.presentation.dto.OrderCreatedResponse;
import com.fashion.ecommerce.presentation.dto.OrderItemRequest;
import com.fashion.ecommerce.presentation.dto.OrderResponse;
import com.fashion.ecommerce.presentation.dto.PayOrderRequest;
import com.fashion.ecommerce.presentation.dto.PaymentResultResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Checkout write side: order creation, simulated payment and order lookup.
 * <p>
 * The order is persisted through the existing entity mechanism (the {@code
 * orders} table is RANGE-partitioned by {@code order_year} with a composite PK
 * {@code (id, order_year)}); {@link Order#prePersist} generates the uuid and the
 * year, and {@link OrderItem#prePersist} / {@link Payment#prePersist} copy the
 * year from the parent order. No alternative insert path is used.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    /**
     * Store prices are PEN soles; the JPA default on Order is USD, so the
     * currency is always set explicitly.
     */
    private static final String CURRENCY = "PEN";

    /**
     * Payment provider used by the MVP payment simulation. Real Stripe/PayPal
     * keys are placeholders (see application-dev.yml), so POST
     * /checkout/orders/{orderNumber}/pay records a successful local payment
     * instead of calling a gateway.
     */
    private static final String SIMULATED_PROVIDER = "SIMULATED";

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.shipping.standard-cost:25.00}")
    private BigDecimal standardShippingCost;

    @Value("${app.shipping.free-threshold:500.00}")
    private BigDecimal freeShippingThreshold;

    @Transactional
    public OrderCreatedResponse createOrder(CreateOrderRequest request) {
        List<LineItem> lines = validateAndPriceItems(request);

        ShippingInfo shipping = resolveShippingMethod(request.shippingMethodId());
        BigDecimal subTotal = lines.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxTotal = lines.stream().map(LineItem::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingCost = shipping.isFree(subTotal) ? BigDecimal.ZERO : shipping.baseCost();
        BigDecimal total = subTotal.add(shippingCost).add(taxTotal);

        String customerJson = toJson(request.customer());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .currencyCode(CURRENCY)
                .subTotal(subTotal.setScale(2, RoundingMode.HALF_UP))
                .discountAmount(BigDecimal.ZERO)
                .shippingCost(shippingCost)
                .taxAmount(taxTotal.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(total.setScale(2, RoundingMode.HALF_UP))
                .shippingMethod(shipping.code())
                .customerNotes(request.notes())
                .shippingAddress(customerJson)
                .billingAddress(customerJson)
                .build();

        for (LineItem line : lines) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .variant(line.variant())
                    .productName(line.product().getName())
                    .variantName(line.variant().getName())
                    .sku(line.variant().getSku())
                    .quantity(line.quantity())
                    .unitPrice(line.unitPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(line.tax())
                    .totalPrice(line.lineTotal())
                    .fulfillmentStatus("PENDING")
                    .build();
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        log.info("Order created: number={}, id={}, total={} {}", saved.getOrderNumber(), saved.getId(),
                saved.getTotalAmount(), saved.getCurrencyCode());
        return new OrderCreatedResponse(saved.getId(), saved.getOrderNumber(), saved.getStatus().name(),
                saved.getTotalAmount(), saved.getCurrencyCode(), null);
    }

    @Transactional
    public PaymentResultResponse payOrder(String orderNumber, PayOrderRequest request) {
        if (request == null || !StringUtils.hasText(request.method())) {
            throw new IllegalArgumentException("Payment method is required");
        }
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderNumber));

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            Payment paid = order.getPayments().stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .findFirst()
                    .orElse(null);
            if (paid != null) {
                return new PaymentResultResponse("PAID", paid.getProcessedAt(),
                        paid.getTransactionId(), order.getOrderNumber());
            }
        }

        // MVP stand-in for a real gateway: record a successful local payment.
        // Replace this block with a real Stripe/PayPal charge once real keys
        // are configured (app.stripe.api-key / app.paypal.client-id).
        String transactionId = "SIM-" + UUID.randomUUID();
        Payment payment = Payment.builder()
                .order(order)
                .paymentProvider(SIMULATED_PROVIDER)
                .paymentMethod(request.method())
                .amount(order.getTotalAmount())
                .currencyCode(CURRENCY)
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId(transactionId)
                .processedAt(LocalDateTime.now())
                .providerResponse("{\"simulated\":true,\"note\":\"MVP stand-in until real Stripe/PayPal keys are configured\"}")
                .build();
        order.getPayments().add(payment);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentMethod(SIMULATED_PROVIDER);
        order.setPaymentIntentId(transactionId);
        order.addStatusHistory(Order.OrderStatus.CONFIRMED, "Payment simulated successfully (MVP)");

        Order saved = orderRepository.save(order);
        log.info("Payment simulated for order {}: transactionId={}", saved.getOrderNumber(), transactionId);
        return new PaymentResultResponse("PAID", payment.getProcessedAt(), transactionId, saved.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderNumber));
        return OrderResponse.from(order, objectMapper);
    }

    /**
     * Validates each line against the DB and computes server-side prices. Client
     * prices are never trusted.
     */
    private List<LineItem> validateAndPriceItems(CreateOrderRequest request) {
        List<OrderItemRequest> items = request.items();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        List<LineItem> lines = new ArrayList<>(items.size());
        for (OrderItemRequest item : items) {
            if (item.productId() == null || item.variantId() == null || item.quantity() == null) {
                throw new IllegalArgumentException("Each item needs productId, variantId and quantity");
            }
            if (item.quantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }
            ProductVariant variant = variantRepository.findById(item.variantId())
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + item.variantId()));
            Product product = variant.getProduct();
            if (!product.getId().equals(item.productId())) {
                throw new IllegalArgumentException("Variant " + item.variantId()
                        + " does not belong to product " + item.productId());
            }
            if (Boolean.FALSE.equals(variant.getIsActive())) {
                throw new IllegalArgumentException("Variant is not active: " + item.variantId());
            }
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getPrice();
            if (unitPrice == null) {
                throw new IllegalArgumentException("Product has no price: " + item.productId());
            }
            int available = variant.getTotalStock();
            if (available < item.quantity()) {
                throw new IllegalStateException("Insufficient stock for variant " + item.variantId()
                        + " (available: " + available + ")");
            }
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            BigDecimal taxRate = product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO;
            BigDecimal tax = lineTotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            lines.add(new LineItem(variant, product, item.quantity(), unitPrice, lineTotal, tax));
        }
        return lines;
    }

    private ShippingInfo resolveShippingMethod(Long shippingMethodId) {
        if (shippingMethodId == null) {
            // Default: REQ-CART-005 standard shipping (config-driven).
            return new ShippingInfo("STANDARD", standardShippingCost, freeShippingThreshold);
        }
        return jdbcTemplate.query(
                "SELECT code, base_cost, free_shipping_threshold FROM shipping_methods "
                        + "WHERE id = ? AND is_active = true",
                rs -> {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Unknown shipping method: " + shippingMethodId);
                    }
                    BigDecimal baseCost = rs.getBigDecimal("base_cost");
                    BigDecimal freeThreshold = rs.getBigDecimal("free_shipping_threshold");
                    return new ShippingInfo(rs.getString("code"),
                            baseCost != null ? baseCost : BigDecimal.ZERO, freeThreshold);
                },
                shippingMethodId);
    }

    private String generateOrderNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "MA-" + (10000000 + ThreadLocalRandom.current().nextInt(90000000));
            if (orderRepository.findByOrderNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique order number");
    }

    private String toJson(CustomerRequest customer) {
        try {
            return objectMapper.writeValueAsString(customer);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize customer address", ex);
        }
    }

    private record LineItem(ProductVariant variant, Product product, int quantity,
                            BigDecimal unitPrice, BigDecimal lineTotal, BigDecimal tax) {
    }

    private record ShippingInfo(String code, BigDecimal baseCost, BigDecimal freeThreshold) {
        boolean isFree(BigDecimal subtotal) {
            return freeThreshold != null && subtotal.compareTo(freeThreshold) >= 0;
        }
    }
}
