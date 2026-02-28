package com.example.rpg.web;

import com.example.rpg.game.GameService;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionGameStore {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public SessionGameStore(Duration ttl) {
        long millis = ttl == null ? 0L : ttl.toMillis();
        this.ttlMillis = Math.max(millis, Duration.ofMinutes(5).toMillis());
    }

    public GameService getOrCreate(String sessionId) {
        cleanupExpired();
        long now = System.currentTimeMillis();
        SessionEntry entry = sessions.compute(sessionId, (key, existing) -> {
            if (existing == null || existing.isExpired(now, ttlMillis)) {
                return new SessionEntry(new GameService(), now);
            }
            existing.touch(now);
            return existing;
        });
        return entry.gameService;
    }

    public String newSessionId() {
        byte[] randomBytes = new byte[24];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public long ttlSeconds() {
        return ttlMillis / 1000L;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, SessionEntry> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired(now, ttlMillis)) {
                sessions.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class SessionEntry {
        private final GameService gameService;
        private volatile long lastAccessedAt;

        private SessionEntry(GameService gameService, long lastAccessedAt) {
            this.gameService = gameService;
            this.lastAccessedAt = lastAccessedAt;
        }

        private boolean isExpired(long now, long ttlMillis) {
            return now - lastAccessedAt > ttlMillis;
        }

        private void touch(long now) {
            this.lastAccessedAt = now;
        }
    }
}
