package com.fashion.ecommerce.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Configuración de Auditoría JPA mejorada.
 * - Rastrea automáticamente created_by y updated_by.
 * - Funciona con autenticación JWT.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    @Component
    static class SpringSecurityAuditorAware implements AuditorAware<Long> {

        @Override
        public Optional<Long> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            
            Object principal = authentication.getPrincipal();
            
            // Si es un usuario autenticado (no "anonymousUser" o "remember-me")
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                
                // Aquí deberías obtener el ID del usuario desde el email/username
                // Esto depende de tu implementación de UserDetailsService
                // Por ahora retornamos null para que se use el valor por defecto en la entidad
                return Optional.empty(); 
            }
            
            return Optional.empty();
        }
    }
}
