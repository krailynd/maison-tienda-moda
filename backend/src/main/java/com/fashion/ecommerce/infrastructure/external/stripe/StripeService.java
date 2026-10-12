package com.fashion.ecommerce.infrastructure.external.stripe;

import com.fashion.ecommerce.domain.model.Order;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${app.stripe.api-key:}")
    private String apiSecret;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiSecret;
    }

    /**
     * Crea un PaymentIntent de Stripe para procesar el pago
     */
    public PaymentIntent createPaymentIntent(Order order, String paymentMethodId) throws StripeException {
        log.info("Creating Stripe PaymentIntent for order: {}", order.getOrderNumber());

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(convertToCents(order.getTotalAmount()))
                .setCurrency(order.getCurrencyCode().toLowerCase())
                .setPaymentMethod(paymentMethodId)
                .setCustomer(order.getUser().getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("order_id", order.getId().toString())
                .putMetadata("order_number", order.getOrderNumber())
                .putMetadata("customer_email", order.getUser().getEmail())
                .setConfirm(true)
                .setReturnUrl("https://fashion-ecommerce.com/order/confirmation")
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.info("Stripe PaymentIntent created: {}", paymentIntent.getId());
        
        return paymentIntent;
    }

    /**
     * Confirma un PaymentIntent existente
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Confirming Stripe PaymentIntent: {}", paymentIntentId);
        
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        
        if (!"requires_confirmation".equals(paymentIntent.getStatus())) {
            throw new IllegalStateException("PaymentIntent is not in requires_confirmation state");
        }

        PaymentIntent confirmedIntent = paymentIntent.confirm();
        log.info("Stripe PaymentIntent confirmed: {}", confirmedIntent.getId());
        
        return confirmedIntent;
    }

    /**
     * Recupera un PaymentIntent por su ID
     */
    public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Procesa un reembolso parcial o total
     */
    public com.stripe.model.Refund refundPayment(String paymentIntentId, BigDecimal amount) throws StripeException {
        log.info("Processing refund for PaymentIntent: {}", paymentIntentId);
        
        Map<String, Object> params = new HashMap<>();
        params.put("payment_intent", paymentIntentId);
        
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            params.put("amount", convertToCents(amount));
        }

        return com.stripe.model.Refund.create(params);
    }

    /**
     * Convierte monto decimal a centavos (Stripe usa centavos)
     */
    private long convertToCents(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100")).longValueExact();
    }

    /**
     * Verifica la firma del webhook de Stripe
     */
    public boolean verifyWebhookSignature(String payload, String sigHeader) {
        try {
            com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(
                    payload, sigHeader, webhookSecret
            );
            log.info("Webhook signature verified for event: {}", event.getType());
            return true;
        } catch (com.stripe.exception.SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Maneja eventos del webhook de Stripe
     */
    public void handleWebhookEvent(com.stripe.model.Event event) {
        String eventType = event.getType();
        log.info("Processing Stripe webhook event: {}", eventType);

        switch (eventType) {
            case "payment_intent.succeeded":
                handlePaymentSucceeded((PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null));
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailed((PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null));
                break;
            case "charge.refunded":
                handleChargeRefunded((PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null));
                break;
            default:
                log.warn("Unhandled Stripe event type: {}", eventType);
        }
    }

    private void handlePaymentSucceeded(PaymentIntent paymentIntent) {
        String orderId = paymentIntent.getMetadata().get("order_id");
        log.info("Payment succeeded for order: {}", orderId);
        // TODO: Actualizar estado de la orden a PAID
        // orderService.markOrderAsPaid(orderId);
    }

    private void handlePaymentFailed(PaymentIntent paymentIntent) {
        String orderId = paymentIntent.getMetadata().get("order_id");
        log.error("Payment failed for order: {}", orderId);
        // TODO: Actualizar estado de la orden a PAYMENT_FAILED
        // orderService.markOrderAsPaymentFailed(orderId);
    }

    private void handleChargeRefunded(PaymentIntent paymentIntent) {
        String orderId = paymentIntent.getMetadata().get("order_id");
        log.info("Charge refunded for order: {}", orderId);
        // TODO: Procesar reembolso de la orden
        // orderService.processRefund(orderId);
    }
}
