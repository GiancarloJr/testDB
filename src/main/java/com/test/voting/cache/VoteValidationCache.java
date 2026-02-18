package com.test.voting.cache;

import com.test.voting.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteValidationCache {

    private final StringRedisTemplate redis;
    private final VoteRepository voteRepository;

    public boolean reserve(Long sessionId, String cpf, Duration ttl) {
        String key = "vote:" + sessionId + ":" + cpf;
        try {
            Boolean voteValidation = redis.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(voteValidation);
        } catch (Exception e) {
            log.error("Redis unavailable when validating duplicate vote. Error: {}", e.getMessage());
            return !voteRepository.existsBySessionIdAndCpf(sessionId, cpf);
        }
    }

    public void release(Long sessionId, String cpf) {
        String key = "vote:" + sessionId + ":" + cpf;
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.error("Failed to release redis reservation. Error: {}", e.getMessage());
        }
    }
}

