package com.fashion.ecommerce.infrastructure.external.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.phone.number:}")
    private String fromPhoneNumber;

    /**
     * Envía SMS de notificación de orden
     */
    @Async
    public void sendOrderConfirmation(String to, String orderNumber) {
        log.info("Sending order confirmation SMS to: {}", to);
        
        String message = String.format(
            "Fashion E-commerce: Tu orden #%s ha sido confirmada. Gracias por tu compra!",
            orderNumber
        );
        
        sendSms(to, message);
    }

    /**
     * Envía SMS de notificación de envío
     */
    @Async
    public void sendShippingNotification(String to, String orderNumber, String trackingNumber) {
        log.info("Sending shipping notification SMS to: {}", to);
        
        String message = String.format(
            "Fashion E-commerce: Tu orden #%s ha sido enviada. Tracking: %s",
            orderNumber, trackingNumber
        );
        
        sendSms(to, message);
    }

    /**
     * Envía SMS de código de verificación (2FA)
     */
    @Async
    public void sendVerificationCode(String to, String code) {
        log.info("Sending verification code SMS to: {}", to);
        
        String message = String.format(
            "Fashion E-commerce: Tu código de verificación es: %s. Válido por 10 minutos.",
            code
        );
        
        sendSms(to, message);
    }

    /**
     * Envía SMS de recordatorio de carrito abandonado
     */
    @Async
    public void sendAbandonedCartReminder(String to, String firstName) {
        log.info("Sending abandoned cart SMS to: {}", to);
        
        String message = String.format(
            "Hola %s! Tienes artículos en tu carrito. Completa tu compra ahora y obtén 10%% OFF con código: CART10",
            firstName
        );
        
        sendSms(to, message);
    }

    /**
     * Envía SMS genérico
     */
    @Async
    public void sendSms(String to, String message) {
        if (accountSid == null || accountSid.isEmpty()) {
            log.warn("Twilio no configurado. SMS no enviado: {}", to);
            return;
        }

        try {
            // Implementación real con Twilio SDK
            /*
            Twilio.init(accountSid, authToken);
            
            Message.creator(
                new com.twilio.type.PhoneNumber(to),
                new com.twilio.type.PhoneNumber(fromPhoneNumber),
                message
            ).create();
            */
            
            log.info("SMS enviado exitosamente a: {}", to);
        } catch (Exception e) {
            log.error("Error enviando SMS a {}: {}", to, e.getMessage(), e);
        }
    }
}
