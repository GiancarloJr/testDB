package com.test.voting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SessionRequest(
    @NotBlank(message = "Description is required")
    String description,
    
    @Positive(message = "Voting time must be positive")
    Integer votingTimeMinutes
) {
}
