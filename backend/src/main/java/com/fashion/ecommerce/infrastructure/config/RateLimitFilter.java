package com.fashion.ecommerce.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Canonical rate-limiting filter (deduplicated, design D5).
 * <p>
 * Token-bucket (Bucket4j): 100 requests per minute per IP. When the budget is
 * exhausted the request is rejected with {@code 429 Too Many Requests} plus a
 * {@code Retry-After} header and the standard error contract
 * (REQ-API-003).
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    public static final long MAX_REQUESTS_PER_MINUTE = 100;

    private static final Bandwidth LIMIT_GENERAL = Bandwidth.classic(
            MAX_REQUESTS_PER_MINUTE,
            Refill.intervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
    );

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, key -> Bucket4j.builder().addLimit(LIMIT_GENERAL).build());

        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = secondsUntilRefill(bucket);
        log.warn("Rate limit exceeded for IP {} on {}", ip, request.getRequestURI());

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        String body = String.format(
                "{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Retry after %d seconds.\"}}",
                retryAfterSeconds);
        response.getWriter().write(body);
    }

    private long secondsUntilRefill(Bucket bucket) {
        long waitNanos = bucket.tryConsumeAndReturnRemaining(1).getNanosToWaitForRefill();
        long seconds = TimeUnit.NANOSECONDS.toSeconds(waitNanos);
        return Math.max(seconds, 1);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
