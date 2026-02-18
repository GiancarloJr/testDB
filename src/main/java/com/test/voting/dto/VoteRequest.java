package com.test.voting.dto;

import com.test.voting.model.enums.VoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VoteRequest(
    @NotNull(message = "Session ID is required")
    Long sessionId,
    
    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "^\\d{11}$", message = "CPF must contain exactly 11 numbers")
    String cpf,
    
    @NotNull(message = "Vote is required")
    VoteType vote
) {
}
