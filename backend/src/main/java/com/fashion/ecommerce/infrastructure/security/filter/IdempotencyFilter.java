package com.fashion.ecommerce.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Idempotency filter (moved to the canonical package, design D7).
 * <p>
 * Honors the {@code Idempotency-Key} header on state-changing requests
 * (POST/PUT/PATCH, REQ-API-006): the response of the first processed request is
 * cached in Redis for 24h and replayed on duplicate keys so replays never
 * double-charge or double-create.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    private static final String KEY_PATTERN = "^[a-zA-Z0-9\\-_]{1,100}$";

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = resolveIdempotencyKey(request);
        if (!StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!idempotencyKey.matches(KEY_PATTERN)) {
            log.warn("Invalid idempotency key format");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Idempotency-Key must be alphanumeric (max 100 characters)");
            return;
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            String cachedResponse = redisTemplate.opsForValue().get(redisKey);
            if (cachedResponse != null) {
                log.info("Replaying cached response for idempotency key: {}", idempotencyKey);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(cachedResponse);
                return;
            }
        }

        IdempotentResponseWrapper responseWrapper = new IdempotentResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        String responseBody = responseWrapper.getResponseBody();
        if (responseBody != null && !responseBody.isEmpty()) {
            redisTemplate.opsForValue().set(
                    redisKey, responseBody, IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Cached response for idempotency key: {}", idempotencyKey);
        }
    }

    private String resolveIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader("Idempotency-Key");
        if (!StringUtils.hasText(key)) {
            key = request.getHeader("X-Idempotency-Key");
        }
        return key;
    }
}
