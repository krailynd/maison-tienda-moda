package com.fashion.ecommerce.infrastructure.external.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Envía email de confirmación de orden
     */
    @Async
    public void sendOrderConfirmation(String to, String orderNumber, Double total) {
        log.info("Sending order confirmation email to: {}", to);
        
        String subject = "Confirmación de Orden #" + orderNumber;
        String content = buildOrderConfirmationHtml(orderNumber, total);
        
        sendHtmlEmail(to, subject, content);
    }

    /**
     * Envía email de carrito abandonado
     */
    @Async
    public void sendAbandonedCartEmail(String to, String firstName, Double cartTotal, String recoveryCode) {
        log.info("Sending abandoned cart email to: {}", to);
        
        String subject = "¡Tu carrito te espera! - 10% de descuento";
        String content = buildAbandonedCartHtml(firstName, cartTotal, recoveryCode);
        
        sendHtmlEmail(to, subject, content);
    }

    /**
     * Envía email de recuperación de contraseña
     */
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetToken) {
        log.info("Sending password reset email to: {}", to);
        
        String resetLink = frontendUrl + "/auth/reset-password?token=" + resetToken;
        String subject = "Recuperación de Contraseña";
        String content = buildPasswordResetHtml(firstName, resetLink);
        
        sendHtmlEmail(to, subject, content);
    }

    /**
     * Envía email de bienvenida
     */
    @Async
    public void sendWelcomeEmail(String to, String firstName, String welcomeCode) {
        log.info("Sending welcome email to: {}", to);
        
        String subject = "¡Bienvenido a Fashion E-commerce!";
        String content = buildWelcomeHtml(firstName, welcomeCode);
        
        sendHtmlEmail(to, subject, content);
    }

    /**
     * Envía email de notificación de envío
     */
    @Async
    public void sendShippingNotification(String to, String orderNumber, String trackingNumber, String carrier) {
        log.info("Sending shipping notification email to: {}", to);
        
        String subject = "Tu orden #" + orderNumber + " ha sido enviada";
        String content = buildShippingNotificationHtml(orderNumber, trackingNumber, carrier);
        
        sendHtmlEmail(to, subject, content);
    }

    /**
     * Envía email de solicitud de reseña
     */
    @Async
    public void sendReviewRequest(String to, String firstName, String productName, Long productId) {
        log.info("Sending review request email to: {}", to);
        
        String reviewLink = frontendUrl + "/products/" + productId + "#reviews";
        String subject = "¿Qué opinas de tu compra?";
        String content = buildReviewRequestHtml(firstName, productName, reviewLink);
        
        sendHtmlEmail(to, subject, content);
    }

    /**
     * Envía email HTML personalizado
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Envía email simple (texto plano)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        
        mailSender.send(message);
        log.info("Simple email sent to: {}", to);
    }

    // ===== PLANTILLAS HTML =====

    private String buildOrderConfirmationHtml(String orderNumber, Double total) {
        return """
        <!DOCTYPE html>
        <html>
        <head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}</style></head>
        <body>
            <h2>¡Gracias por tu compra!</h2>
            <p>Tu orden <strong>#%s</strong> ha sido confirmada.</p>
            <p>Total pagado: <strong>$%.2f</strong></p>
            <p>Te enviaremos un email cuando tu orden sea despachada.</p>
            <br>
            <p>Saludos,<br>Equipo Fashion E-commerce</p>
        </body>
        </html>
        """.formatted(orderNumber, total);
    }

    private String buildAbandonedCartHtml(String firstName, Double cartTotal, String recoveryCode) {
        return """
        <!DOCTYPE html>
        <html>
        <head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}.coupon{background:#f0f0f0;padding:15px;border-radius:5px;text-align:center;font-size:20px;font-weight:bold;}</style></head>
        <body>
            <h2>¡Hola %s!</h2>
            <p>Vimos que dejaste algunos artículos en tu carrito.</p>
            <p>¡No te preocupes! Aquí tienes un <strong>10%% de descuento</strong> para completar tu compra:</p>
            <div class="coupon">Código: %s</div>
            <p>Total de tu carrito: $%.2f</p>
            <br>
            <a href="%s/cart" style="background:#000;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;">Volver al carrito</a>
        </body>
        </html>
        """.formatted(firstName, recoveryCode, cartTotal, frontendUrl);
    }

    private String buildPasswordResetHtml(String firstName, String resetLink) {
        return """
        <!DOCTYPE html>
        <html>
        <head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}</style></head>
        <body>
            <h2>Hola %s</h2>
            <p>Recibimos una solicitud para restablecer tu contraseña.</p>
            <p>Haz clic en el botón abaixo para crear una nueva contraseña:</p>
            <br>
            <a href="%s" style="background:#000;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;">Restablecer Contraseña</a>
            <br><br>
            <p>Este enlace expira en 1 hora.</p>
            <p>Si no solicitaste esto, ignora este email.</p>
        </body>
        </html>
        """.formatted(firstName, resetLink);
    }

    private String buildWelcomeHtml(String firstName, String welcomeCode) {
        return """
        <!DOCTYPE html>
        <html>
        <head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}</style></head>
        <body>
            <h2>¡Bienvenido %s!</h2>
            <p>Gracias por registrarte en Fashion E-commerce.</p>
            <p>Como regalo de bienvenida, aquí tienes un código de descuento:</p>
            <h3 style="color:#d32f2f;">%s</h3>
            <p>Úsalo en tu primera compra y obtén 10%% de descuento.</p>
            <br>
            <a href="%s/products" style="background:#000;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;">Explorar Productos</a>
        </body>
        </html>
        """.formatted(firstName, welcomeCode, frontendUrl);
    }

    private String buildShippingNotificationHtml(String orderNumber, String trackingNumber, String carrier) {
        return """
        <!DOCTYPE html>
        <html>
        <head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}</style></head>
        <body>
            <h2>¡Tu orden está en camino!</h2>
            <p>Tu orden <strong>#%s</strong> ha sido despachada.</p>
            <p><strong>Transportista:</strong> %s</p>
            <p><strong>Número de seguimiento:</strong> %s</p>
            <br>
            <p>Puedes rastrear tu pedido usando el número de seguimiento proporcionado.</p>
            <br>
            <p>Gracias por comprar con nosotros,</p>
            <p>Equipo Fashion E-commerce</p>
        </body>
        </html>
        """.formatted(orderNumber, carrier, trackingNumber);
    }

    private String buildReviewRequestHtml(String firstName, String productName, String reviewLink) {
        return """
        <!DOCTYPE html>
        <html>
        <head><style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}</style></head>
        <body>
            <h2>Hola %s</h2>
            <p>Esperamos que estés disfrutando de tu compra: <strong>%s</strong></p>
            <p>Tu opinión es muy importante para nosotros. ¿Podrías tomarte un minuto para dejar una reseña?</p>
            <br>
            <a href="%s" style="background:#000;color:#fff;padding:10px 20px;text-decoration:none;border-radius:5px;">Dejar Reseña</a>
            <br><br>
            <p>¡Gracias por ayudarnos a mejorar!</p>
        </body>
        </html>
        """.formatted(firstName, productName, reviewLink);
    }
}
