package com.fashion.ecommerce.domain.repository;

/**
 * Port for refresh-token single-use storage (design: jti rotation).
 * <p>
 * Keys are {@code auth:refresh:{jti}} mapping to the owner's email. A refresh
 * is only accepted once: {@link #consume(String)} atomically removes the key
 * on success, so any later attempt with the same jti (token reuse) fails.
 */
public interface RefreshTokenStore {

    /**
     * Persists a refresh-token jti bound to the owning email, with the
     * configured lifetime (7 days by default).
     */
    void store(String jti, String email);

    /**
     * Atomically checks-and-deletes the jti. Returns {@code true} exactly once
     * per stored jti; subsequent calls for the same jti return {@code false}
     * (reuse detection).
     */
    boolean consume(String jti);

    /**
     * Deletes the jti (explicit logout / token revocation).
     */
    void revoke(String jti);
}
