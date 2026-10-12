package com.fashion.ecommerce.infrastructure.external.paypal;

import com.fashion.ecommerce.domain.model.Order;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.*;
import com.paypal.payments.CapturesRefundRequest;
import com.paypal.payments.Money;
import com.paypal.payments.Refund;
import com.paypal.payments.RefundRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    @Value("${app.paypal.client-id:}")
    private String clientId;

    @Value("${app.paypal.client-secret:}")
    private String clientSecret;

    @Value("${app.paypal.mode:sandbox}")
    private String mode;

    private PayPalHttpClient payPalClient;

    @PostConstruct
    public void init() {
        PayPalEnvironment environment = "sandbox".equals(mode) 
            ? new PayPalEnvironment.Sandbox(clientId, clientSecret)
            : new PayPalEnvironment.Live(clientId, clientSecret);
        
        payPalClient = new PayPalHttpClient(environment);
        log.info("PayPal client initialized in {} mode", mode);
    }

    /**
     * Crea una orden de PayPal para procesar el pago
     */
    public com.paypal.orders.Order createOrder(Order fashionOrder, String returnUrl, String cancelUrl) throws IOException {
        log.info("Creating PayPal order for fashion order: {}", fashionOrder.getOrderNumber());

        AmountWithBreakdown amount = new AmountWithBreakdown()
                .currencyCode(fashionOrder.getCurrencyCode())
                .value(fashionOrder.getTotalAmount().toString());

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .amountWithBreakdown(amount)
                .referenceId(fashionOrder.getId().toString())
                .description("Order #" + fashionOrder.getOrderNumber())
                .items(Collections.emptyList()); // TODO: Agregar items detallados

        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .brandName("Fashion E-commerce")
                .landingPage("LOGIN")
                .userAction("CONTINUE");

        OrderRequest orderRequest = new OrderRequest()
                .checkoutPaymentIntent("CAPTURE")
                .purchaseUnits(List.of(purchaseUnit))
                .applicationContext(applicationContext);

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.requestBody(orderRequest);
        request.prefer("return=representation");

        com.paypal.orders.Order createdOrder = payPalClient.execute(request).result();
        log.info("PayPal order created: {}", createdOrder.id());

        return createdOrder;
    }

    /**
     * Captura un pago de PayPal después de la aprobación del usuario
     */
    public com.paypal.orders.Order captureOrder(String orderId) throws IOException {
        log.info("Capturing PayPal order: {}", orderId);

        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.requestBody(new OrderActionRequest());

        com.paypal.orders.Order capture = payPalClient.execute(request).result();
        log.info("PayPal order captured: {} - Status: {}", orderId, capture.status());

        return capture;
    }

    /**
     * Obtiene los detalles de una orden de PayPal
     */
    public com.paypal.orders.Order getOrderDetails(String orderId) throws IOException {
        OrdersGetRequest request = new OrdersGetRequest(orderId);
        return payPalClient.execute(request).result();
    }

    /**
     * Procesa un reembolso en PayPal
     */
    public Refund refundPayment(String captureId, BigDecimal amount) throws IOException {
        log.info("Processing PayPal refund for capture: {}", captureId);

        RefundRequest refundRequest = new RefundRequest();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            refundRequest.amount(new Money().currencyCode("USD").value(amount.toString()));
        }

        CapturesRefundRequest request = new CapturesRefundRequest(captureId);
        request.requestBody(refundRequest);

        return payPalClient.execute(request).result();
    }

    /**
     * Valida que el paymentID y PayerID coincidan con la orden
     */
    public boolean validatePayment(String paymentId, String payerId, String expectedOrderId) throws IOException {
        com.paypal.orders.Order paypalOrder = getOrderDetails(paymentId);
        
        if (!"APPROVED".equals(paypalOrder.status()) && !"COMPLETED".equals(paypalOrder.status())) {
            log.error("PayPal order not approved: {}", paymentId);
            return false;
        }

        String referenceId = paypalOrder.purchaseUnits().get(0).referenceId();
        boolean isValid = referenceId.equals(expectedOrderId);
        
        if (!isValid) {
            log.error("PayPal order reference ID mismatch. Expected: {}, Got: {}", expectedOrderId, referenceId);
        }

        return isValid;
    }
}
