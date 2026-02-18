package com.test.voting.cache;

import com.test.voting.dto.SessionCache;
import com.test.voting.model.Session;
import com.test.voting.model.enums.SessionStatus;
import com.test.voting.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionValidationCacheTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SessionService sessionService;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private Clock clock;

    @InjectMocks
    private SessionValidationCache sessionCache;

    private final Instant TIME_NOW = Instant.parse("2026-02-17T12:00:00Z");

    @BeforeEach
    void setUp() {

        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldReturnFromCacheWhenPresent() {
        Instant expiresAt = Instant.parse("2026-02-17T13:00:00Z");
        String json = "{\"status\":\"OPEN\",\"expiresAt\":\"2026-02-17T13:00:00Z\"}";

        SessionCache expected = new SessionCache(SessionStatus.OPEN, expiresAt);

        when(valueOps.get("session:1")).thenReturn(json);
        when(objectMapper.readValue(json,SessionCache.class))
                .thenReturn(expected);

        SessionCache result = sessionCache.get(1L);

        assertThat(result).isEqualTo(expected);

        verifyNoInteractions(sessionService);
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void shouldFallbackToDbWhenCacheMiss_andWriteWithExpectedTtl() {
        Instant expiresAt = TIME_NOW.plus(Duration.ofHours(1));

        Session session = Session.builder()
                .id(1L)
                .status(SessionStatus.OPEN)
                .expirationTime(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                .build();

        when(valueOps.get("session:1")).thenReturn(null);
        when(sessionService.findById(1L)).thenReturn(session);
        when(objectMapper.writeValueAsString(any(SessionCache.class))).thenReturn("{\"ok\":true}");
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-02-17T12:00:00Z"));

        SessionCache result = sessionCache.get(1L);

        assertThat(result.status()).isEqualTo(SessionStatus.OPEN);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("session:1"), eq("{\"ok\":true}"), ttlCaptor.capture());

        Duration expectedTtl = Duration.between(TIME_NOW, expiresAt).plusSeconds(60);
        assertThat(ttlCaptor.getValue()).isEqualTo(expectedTtl);

        verify(sessionService).findById(1L);
    }

    @Test
    void shouldFallbackToDbWhenCacheParsingFails_andRewriteCache() {
        Instant expiresAt = TIME_NOW.plus(Duration.ofMinutes(5));

        Session session = Session.builder()
                .id(1L)
                .status(SessionStatus.CLOSE)
                .expirationTime(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
                .build();

        when(valueOps.get("session:1")).thenReturn("invalid-json");
        when(objectMapper.readValue(eq("invalid-json"), eq(SessionCache.class)))
                .thenThrow(new RuntimeException("parse error"));

        when(sessionService.findById(1L)).thenReturn(session);
        when(objectMapper.writeValueAsString(any(SessionCache.class))).thenReturn("{}");
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(Instant.parse("2026-02-17T12:00:00Z"));

        SessionCache result = sessionCache.get(1L);

        assertThat(result.status()).isEqualTo(SessionStatus.CLOSE);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);

        verify(sessionService).findById(1L);
        verify(valueOps).set(eq("session:1"), eq("{}"), any(Duration.class));
    }



}
