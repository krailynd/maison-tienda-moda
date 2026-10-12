package com.fashion.ecommerce.infrastructure.messaging.listener;

import com.fashion.ecommerce.infrastructure.external.email.EmailService;
import com.fashion.ecommerce.infrastructure.external.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventListener {

    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Escucha eventos de orden creada
     */
    @RabbitListener(queues = "${rabbitmq.queue.order-created:order.created}")
    public void handleOrderCreated(Map<String, Object> orderData) {
        log.info("Received order.created event: {}", orderData);

        String email = (String) orderData.get("customerEmail");
        String orderNumber = (String) orderData.get("orderNumber");
        Double total = ((Number) orderData.get("total")).doubleValue();

        // Enviar email de confirmación
        emailService.sendOrderConfirmation(email, orderNumber, total);

        // Enviar SMS de confirmación
        String phone = (String) orderData.get("customerPhone");
        if (phone != null && !phone.isEmpty()) {
            smsService.sendOrderConfirmation(phone, orderNumber);
        }
    }

    /**
     * Escucha eventos de orden pagada
     */
    @RabbitListener(queues = "${rabbitmq.queue.order-paid:order.paid}")
    public void handleOrderPaid(Map<String, Object> orderData) {
        log.info("Received order.paid event: {}", orderData);

        // TODO: Actualizar inventario
        // TODO: Notificar al almacén para preparación
    }

    /**
     * Escucha eventos de orden enviada
     */
    @RabbitListener(queues = "${rabbitmq.queue.order-shipped:order.shipped}")
    public void handleOrderShipped(Map<String, Object> orderData) {
        log.info("Received order.shipped event: {}", orderData);

        String email = (String) orderData.get("customerEmail");
        String orderNumber = (String) orderData.get("orderNumber");
        String trackingNumber = (String) orderData.get("trackingNumber");
        String carrier = (String) orderData.get("carrier");

        // Enviar email de notificación de envío
        emailService.sendShippingNotification(email, orderNumber, trackingNumber, carrier);

        // Enviar SMS de notificación
        String phone = (String) orderData.get("customerPhone");
        if (phone != null && !phone.isEmpty()) {
            smsService.sendShippingNotification(phone, orderNumber, trackingNumber);
        }
    }

    /**
     * Escucha eventos de carrito abandonado
     */
    @RabbitListener(queues = "${rabbitmq.queue.cart-abandoned:cart.abandoned}")
    public void handleCartAbandoned(Map<String, Object> cartData) {
        log.info("Received cart.abandoned event: {}", cartData);

        String email = (String) cartData.get("customerEmail");
        String firstName = (String) cartData.get("customerFirstName");
        Double cartTotal = ((Number) cartData.get("cartTotal")).doubleValue();
        String recoveryCode = (String) cartData.get("recoveryCode");

        // Enviar email de recuperación con descuento
        emailService.sendAbandonedCartEmail(email, firstName, cartTotal, recoveryCode);

        // Opcional: Enviar SMS de recordatorio
        // smsService.sendAbandonedCartReminder(phone, firstName);
    }

    /**
     * Escucha eventos de stock bajo
     */
    @RabbitListener(queues = "${rabbitmq.queue.stock-low:stock.low}")
    public void handleStockLow(Map<String, Object> stockData) {
        log.info("Received stock.low event: {}", stockData);

        String productName = (String) stockData.get("productName");
        String sku = (String) stockData.get("sku");
        Integer currentStock = (Integer) stockData.get("currentStock");
        Integer reorderPoint = (Integer) stockData.get("reorderPoint");

        log.warn("STOCK BAJO DETECTADO: Producto '{}' (SKU: {}) tiene {} unidades. Punto de reorden: {}", 
                productName, sku, currentStock, reorderPoint);

        // TODO: Enviar email al equipo de compras/inventario
        // emailService.sendLowStockAlert(adminEmail, productName, sku, currentStock);
    }
}
