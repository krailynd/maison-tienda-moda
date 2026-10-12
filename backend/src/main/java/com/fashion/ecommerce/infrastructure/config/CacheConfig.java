package com.fashion.ecommerce.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Multi-level cache configuration (moved to the canonical package, design D7).
 * <ul>
 *   <li>Level 1: Caffeine (local in-memory, ultra fast).</li>
 *   <li>Level 2: Redis (distributed, shared across instances).</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

        RedisCacheConfiguration productsConfig = defaultConfig.entryTtl(Duration.ofHours(2));
        RedisCacheConfiguration categoriesConfig = defaultConfig.entryTtl(Duration.ofHours(24));
        RedisCacheConfiguration userConfig = defaultConfig.entryTtl(Duration.ofMinutes(15));
        RedisCacheConfiguration cartConfig = defaultConfig.entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("products", productsConfig)
            .withCacheConfiguration("categories", categoriesConfig)
            .withCacheConfiguration("users", userConfig)
            .withCacheConfiguration("carts", cartConfig)
            .transactionAware()
            .build();
    }

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats());

        cacheManager.setCacheNames(Arrays.asList(
            "configurations",
            "exchange-rates",
            "shipping-rates",
            "tax-rates"
        ));

        return cacheManager;
    }
}
