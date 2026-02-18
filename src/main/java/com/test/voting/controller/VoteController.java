package com.test.voting.controller;

import com.test.voting.dto.VoteRequest;
import com.test.voting.dto.VoteResponse;
import com.test.voting.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Slf4j
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<VoteResponse> registerVote(@Valid @RequestBody VoteRequest request) {
        VoteResponse voteResponse = voteService.registerVote(request);

        return ResponseEntity
                .status(voteResponse.httpStatus())
                .body(voteResponse);
    }
}
