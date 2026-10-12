package com.fashion.ecommerce.infrastructure.security;

import com.fashion.ecommerce.domain.repository.RefreshTokenStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only in-memory {@link RefreshTokenStore} used by unit tests (the real
 * implementation is {@link RedisRefreshTokenStore}).
 */
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    @Override
    public void store(String jti, String email) {
        tokens.put(jti, email);
    }

    @Override
    public boolean consume(String jti) {
        return tokens.remove(jti) != null;
    }

    @Override
    public void revoke(String jti) {
        tokens.remove(jti);
    }
}
