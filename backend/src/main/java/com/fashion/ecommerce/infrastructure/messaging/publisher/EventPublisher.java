package com.fashion.ecommerce.infrastructure.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.fashion:fashion.exchange}")
    private String fashionExchange;

    /**
     * Publica evento de orden creada
     */
    public void publishOrderCreated(Object orderData) {
        log.info("Publishing order.created event");
        rabbitTemplate.convertAndSend(fashionExchange, "order.created", orderData);
    }

    /**
     * Publica evento de orden pagada
     */
    public void publishOrderPaid(Object orderData) {
        log.info("Publishing order.paid event");
        rabbitTemplate.convertAndSend(fashionExchange, "order.paid", orderData);
    }

    /**
     * Publica evento de orden enviada
     */
    public void publishOrderShipped(Object orderData) {
        log.info("Publishing order.shipped event");
        rabbitTemplate.convertAndSend(fashionExchange, "order.shipped", orderData);
    }

    /**
     * Publica evento de carrito abandonado
     */
    public void publishCartAbandoned(Object cartData) {
        log.info("Publishing cart.abandoned event");
        rabbitTemplate.convertAndSend(fashionExchange, "cart.abandoned", cartData);
    }

    /**
     * Publica evento de stock bajo
     */
    public void publishStockLow(Object stockData) {
        log.info("Publishing stock.low event");
        rabbitTemplate.convertAndSend(fashionExchange, "stock.low", stockData);
    }

    /**
     * Publica evento genérico con routing key personalizada
     */
    public void publishEvent(String routingKey, Object data) {
        log.info("Publishing event with routing key: {}", routingKey);
        rabbitTemplate.convertAndSend(fashionExchange, routingKey, data);
    }
}
