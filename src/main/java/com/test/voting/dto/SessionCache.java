package com.test.voting.dto;

import com.test.voting.model.enums.SessionStatus;

import java.time.Instant;

public record SessionCache(SessionStatus status, Instant expiresAt) {

    public boolean closedOrExpired(Instant now) {
        return SessionStatus.CLOSE.equals(status) || expiresAt.isBefore(now);
    }
}

