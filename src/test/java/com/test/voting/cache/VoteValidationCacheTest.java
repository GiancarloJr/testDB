package com.test.voting.cache;

import com.test.voting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteValidationCacheTest {

    private static final Long SESSION_ID = 1L;
    private static final String CPF = "12345678909";
    private static final Duration TTL = Duration.ofHours(1);

    private static final String KEY = "vote:1:12345678909";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private VoteValidationCache voteValidationCache;

    @Test
    void shouldReturnTrueWhenReserveSucceeds() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(KEY, "1", TTL)).thenReturn(true);

        boolean result = voteValidationCache.reserve(SESSION_ID, CPF, TTL);

        assertThat(result).isTrue();
        verify(valueOps).setIfAbsent(KEY, "1", TTL);
        verifyNoInteractions(voteRepository);
    }

    @Test
    void shouldReturnFalseWhenAlreadyReserved() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(KEY, "1", TTL)).thenReturn(false);

        boolean result = voteValidationCache.reserve(SESSION_ID, CPF, TTL);

        assertThat(result).isFalse();
        verify(valueOps).setIfAbsent(KEY, "1", TTL);
        verifyNoInteractions(voteRepository);
    }

    @Test
    void shouldFallbackToDbAndReturnTrueWhenRedisFailsAndVoteDoesNotExist() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(KEY, "1", TTL)).thenThrow(new RuntimeException("Redis Down"));
        when(voteRepository.existsBySessionIdAndCpf(SESSION_ID, CPF)).thenReturn(false);

        boolean result = voteValidationCache.reserve(SESSION_ID, CPF, TTL);

        assertThat(result).isTrue();
        verify(voteRepository).existsBySessionIdAndCpf(SESSION_ID, CPF);
    }

    @Test
    void shouldFallbackToDbAndReturnFalseWhenRedisFailsAndVoteAlreadyExists() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(KEY, "1", TTL)).thenThrow(new RuntimeException("Redis Down"));
        when(voteRepository.existsBySessionIdAndCpf(SESSION_ID, CPF)).thenReturn(true);

        boolean result = voteValidationCache.reserve(SESSION_ID, CPF, TTL);

        assertThat(result).isFalse();
        verify(voteRepository).existsBySessionIdAndCpf(SESSION_ID, CPF);
    }

    @Test
    void shouldDeleteKeyOnRelease() {
        voteValidationCache.release(SESSION_ID, CPF);

        verify(redis).delete(KEY);
        verifyNoMoreInteractions(redis);
    }
}
