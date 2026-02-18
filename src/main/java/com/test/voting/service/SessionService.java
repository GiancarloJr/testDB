package com.test.voting.service;

import com.test.voting.dto.ResultResponse;
import com.test.voting.dto.SessionRequest;
import com.test.voting.dto.SessionResponse;
import com.test.voting.exception.ResourceNotFoundException;
import com.test.voting.mapper.SessionMapper;
import com.test.voting.model.Session;
import com.test.voting.repository.SessionRepository;
import com.test.voting.repository.VoteRepository;
import com.test.voting.scheduler.SessionSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final SessionMapper sessionMapper;
    private final SessionSchedulerService sessionSchedulerService;

    @Transactional
    public SessionResponse create(SessionRequest request) {
        log.info("Creating new session: {}", request.description());

        Session session = sessionMapper.toEntity(request);
        session = sessionRepository.save(session);

        sessionSchedulerService.scheduleSessionClosure(session);
        
        log.info("Session created successfully. ID: {}, Status: {}, Expires at: {}",
                session.getId(), session.getStatus(), session.getExpirationTime());
        
        return sessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public ResultResponse getResult(Long sessionId) {
        log.info("Fetching result for session: {}", sessionId);

        Session session = findById(sessionId);

        ResultResponse.VoteCount result = voteRepository.countVotesBySessionId(sessionId);

        ResultResponse.VoteCount voteCount = new ResultResponse.VoteCount(
                result.yes(),
                result.no(),
                result.total()
        );

        return new ResultResponse(
                sessionId,
                voteCount,
                session.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public Session findById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + id));
    }
}
