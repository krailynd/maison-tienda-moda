package com.fashion.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Fashion E-Commerce Application
 * 
 * Tienda virtual de ropa de moda construida con Spring Boot 3.x
 * 
 * @author Fashion E-Commerce Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan("com.fashion.ecommerce")
public class FashionEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FashionEcommerceApplication.class, args);
    }
}
