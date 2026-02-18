package com.test.voting.controller;

import com.test.voting.dto.ResultResponse;
import com.test.voting.dto.SessionResponse;
import com.test.voting.dto.SessionRequest;
import com.test.voting.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        SessionResponse response = sessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/result/{sessionId}")
    public ResponseEntity<ResultResponse> getResult(@PathVariable Long sessionId) {
        ResultResponse response = sessionService.getResult(sessionId);
        return ResponseEntity.ok(response);
    }
}
