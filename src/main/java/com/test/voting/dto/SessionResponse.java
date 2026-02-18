package com.test.voting.dto;

import com.test.voting.model.enums.SessionStatus;

import java.time.LocalDateTime;

public record SessionResponse(
    Long sessionId,
    LocalDateTime creationTime,
    LocalDateTime expirationTime,
    SessionStatus status
) {
}
