package com.test.voting.dto;

import com.test.voting.model.enums.VoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
    @NotNull(message = "Session ID is required")
    Long sessionId,
    
    @NotBlank(message = "CPF is required")
    String cpf,
    
    @NotNull(message = "Vote is required")
    VoteType vote
) {
}
