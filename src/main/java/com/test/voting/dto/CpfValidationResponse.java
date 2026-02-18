package com.test.voting.dto;

import com.test.voting.model.enums.VoteStatus;

public record CpfValidationResponse(
    VoteStatus status
) {
}
