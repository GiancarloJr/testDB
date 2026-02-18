package com.test.voting.dto;

import com.test.voting.model.enums.SessionStatus;

public record ResultResponse(
    Long sessionId,
    VoteCount voteCount,
    SessionStatus sessionStatus
) {
    public record VoteCount(
        Long yes,
        Long no,
        Long total
    ) {
    }
}
