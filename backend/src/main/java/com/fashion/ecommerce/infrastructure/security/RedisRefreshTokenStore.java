package com.fashion.ecommerce.infrastructure.security;

import com.fashion.ecommerce.domain.repository.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis-backed {@link RefreshTokenStore}.
 * <p>
 * Keys {@code auth:refresh:{jti}} hold the owner's email with a 7-day TTL
 * (aligned with {@code app.jwt.refresh-expiration-ms}). Consumption is a
 * single atomic Lua script (EXISTS + DEL) so concurrent refreshes with the
 * same jti can never both succeed — the second one is always a 401 reuse.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    /**
     * Atomic check-and-delete: returns 1 only if the key still exists (i.e.
     * the token was not consumed yet), otherwise 0. The DEL and the check are
     * one Redis round-trip, so reuse can never race a legitimate refresh.
     */
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 then redis.call('DEL', KEYS[1]) return 1 else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Override
    public void store(String jti, String email) {
        stringRedisTemplate.opsForValue().set(key(jti), email, Duration.ofMillis(refreshExpirationMs));
        log.debug("Refresh token stored: jti={}", jti);
    }

    @Override
    public boolean consume(String jti) {
        Long result = stringRedisTemplate.execute(CONSUME_SCRIPT, List.of(key(jti)));
        boolean consumed = Long.valueOf(1L).equals(result);
        log.debug("Refresh token consumed: jti={} result={}", jti, result);
        return consumed;
    }

    @Override
    public void revoke(String jti) {
        stringRedisTemplate.delete(key(jti));
        log.debug("Refresh token revoked: jti={}", jti);
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
