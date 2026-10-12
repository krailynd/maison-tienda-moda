package com.fashion.ecommerce.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.order-created:order.created}")
    private String orderCreatedQueue;

    @Value("${rabbitmq.queue.order-paid:order.paid}")
    private String orderPaidQueue;

    @Value("${rabbitmq.queue.order-shipped:order.shipped}")
    private String orderShippedQueue;

    @Value("${rabbitmq.queue.cart-abandoned:cart.abandoned}")
    private String cartAbandonedQueue;

    @Value("${rabbitmq.queue.stock-low:stock.low}")
    private String stockLowQueue;

    @Value("${rabbitmq.exchange.fashion:fashion.exchange}")
    private String fashionExchange;

    /**
     * Exchange principal tipo TOPIC para enrutar eventos
     */
    @Bean
    public TopicExchange fashionExchange() {
        return new TopicExchange(fashionExchange);
    }

    /**
     * Cola para eventos de orden creada
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue)
                .withArgument("x-message-ttl", 86400000) // 24 horas
                .build();
    }

    /**
     * Cola para eventos de orden pagada
     */
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(orderPaidQueue)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    /**
     * Cola para eventos de orden enviada
     */
    @Bean
    public Queue orderShippedQueue() {
        return QueueBuilder.durable(orderShippedQueue)
                .withArgument("x-message-ttl", 604800000) // 7 días
                .build();
    }

    /**
     * Cola para carritos abandonados
     */
    @Bean
    public Queue cartAbandonedQueue() {
        return QueueBuilder.durable(cartAbandonedQueue)
                .withArgument("x-message-ttl", 3600000) // 1 hora
                .build();
    }

    /**
     * Cola para alertas de stock bajo
     */
    @Bean
    public Queue stockLowQueue() {
        return QueueBuilder.durable(stockLowQueue)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    /**
     * Bindings entre exchange y colas
     */
    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange fashionExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(fashionExchange)
                .with("order.created");
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange fashionExchange) {
        return BindingBuilder.bind(orderPaidQueue)
                .to(fashionExchange)
                .with("order.paid");
    }

    @Bean
    public Binding orderShippedBinding(Queue orderShippedQueue, TopicExchange fashionExchange) {
        return BindingBuilder.bind(orderShippedQueue)
                .to(fashionExchange)
                .with("order.shipped");
    }

    @Bean
    public Binding cartAbandonedBinding(Queue cartAbandonedQueue, TopicExchange fashionExchange) {
        return BindingBuilder.bind(cartAbandonedQueue)
                .to(fashionExchange)
                .with("cart.abandoned");
    }

    @Bean
    public Binding stockLowBinding(Queue stockLowQueue, TopicExchange fashionExchange) {
        return BindingBuilder.bind(stockLowQueue)
                .to(fashionExchange)
                .with("stock.low");
    }

    /**
     * Conversor de mensajes a JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate configurado con conversor JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
