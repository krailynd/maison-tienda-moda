package com.fashion.ecommerce.presentation.controller;

import com.fashion.ecommerce.application.service.CheckoutService;
import com.fashion.ecommerce.presentation.dto.CreateOrderRequest;
import com.fashion.ecommerce.presentation.dto.OrderCreatedResponse;
import com.fashion.ecommerce.presentation.dto.OrderResponse;
import com.fashion.ecommerce.presentation.dto.PayOrderRequest;
import com.fashion.ecommerce.presentation.dto.PaymentResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public checkout and order-lookup endpoints (no auth in the MVP).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/checkout/orders")
    public OrderCreatedResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return checkoutService.createOrder(request);
    }

    @PostMapping("/checkout/orders/{orderNumber}/pay")
    public PaymentResultResponse payOrder(@PathVariable String orderNumber,
                                          @Valid @RequestBody PayOrderRequest request) {
        return checkoutService.payOrder(orderNumber, request);
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderResponse getOrder(@PathVariable String orderNumber) {
        return checkoutService.getOrder(orderNumber);
    }
}
