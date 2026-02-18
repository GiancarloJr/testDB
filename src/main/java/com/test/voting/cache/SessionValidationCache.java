package com.test.voting.cache;

import com.test.voting.dto.SessionCache;
import com.test.voting.model.Session;
import com.test.voting.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionValidationCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final Clock clock;

    private static final long EXTRA_TTL_SECONDS = 60;
    private static final Duration MIN_TTL = Duration.ofMinutes(1);


    public SessionCache get(Long sessionId) {
        String key = "session:" + sessionId;

        return getFromCache(key)
                .orElseGet(() -> fetchFromDbAndCache(sessionId, key));
    }

    private Optional<SessionCache> getFromCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();

            return Optional.of(objectMapper.readValue(json, SessionCache.class));
        } catch (Exception e) {
            log.error("Error reading from Redis cache (key={}). Falling back to the database.", key, e);
            return Optional.empty();
        }
    }

    private SessionCache fetchFromDbAndCache(Long sessionId, String key) {
        Session session = sessionService.findById(sessionId);
        SessionCache sessionCache = toSessionCache(session);

        saveToCache(key, sessionCache);

        return sessionCache;
    }

    private SessionCache toSessionCache(Session session) {
        Instant expiresAt = session.getExpirationTime()
                .atZone(clock.getZone())
                .toInstant();
        return new SessionCache(session.getStatus(), expiresAt);
    }


    private void saveToCache(String key, SessionCache sessionCache) {
        try {
            String json = objectMapper.writeValueAsString(sessionCache);
            redis.opsForValue().set(key, json, calculateTtl(sessionCache.expiresAt()));
        } catch (Exception e) {
            log.error("Failed to update Redis cache for key={}: {}", key, e.getMessage());
        }
    }

    private Duration calculateTtl(Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt).plusSeconds(EXTRA_TTL_SECONDS);
        return (ttl.isNegative() || ttl.isZero()) ? MIN_TTL : ttl;
    }
}
