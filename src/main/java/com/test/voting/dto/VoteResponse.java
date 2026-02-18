package com.test.voting.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.test.voting.model.enums.VoteStatus;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VoteResponse(
        @JsonIgnore
        HttpStatus httpStatus,
        String message,
        VoteStatus status
) {
    public static VoteResponse ok() {
        return new VoteResponse(HttpStatus.ACCEPTED, "Vote registered successfully.", null);
    }

    public static VoteResponse invalidCpf() {
        return new VoteResponse(HttpStatus.NOT_FOUND, "Invalid cpf", null);
    }

    public static VoteResponse alreadyVoted() {
        return new VoteResponse(HttpStatus.CONFLICT, "This CPF has already voted in this session.", null);
    }

    public static VoteResponse unableToVote() {
        return new VoteResponse(HttpStatus.NOT_FOUND, null, VoteStatus.UNABLE_TO_VOTE);
    }

    public static VoteResponse invalidSession() {
        return new VoteResponse(HttpStatus.CONFLICT, "Session expired or closed.", null);
    }
}
