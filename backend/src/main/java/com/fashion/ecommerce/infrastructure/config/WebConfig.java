package com.fashion.ecommerce.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves locally uploaded product images (design D4) at {@code /uploads/**}.
 * <p>
 * A dedicated resource handler ({@code file:{dir}/}) is used instead of
 * {@code spring.web.resources.static-locations} so the classpath static
 * locations are NOT displaced. The directory itself is created lazily by
 * {@link com.fashion.ecommerce.application.service.AdminImageService} on the
 * first write.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:./uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsDir + "/");
    }
}
