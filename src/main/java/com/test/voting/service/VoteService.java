package com.test.voting.service;

import com.test.voting.cache.SessionValidationCache;
import com.test.voting.cache.VoteValidationCache;
import com.test.voting.dto.SessionCache;
import com.test.voting.dto.CpfValidationResponse;
import com.test.voting.dto.VoteMessage;
import com.test.voting.messaging.VoteProducer;
import com.test.voting.dto.VoteRequest;
import com.test.voting.dto.VoteResponse;
import com.test.voting.model.enums.VoteStatus;
import com.test.voting.model.enums.VoteType;
import com.test.voting.facade.CpfValidationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.Instant;

import static com.test.voting.utils.CpfUtils.formatCpf;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteProducer voteProducer;
    private final SessionValidationCache sessionValidationCache;
    private final CpfValidationFacade cpfValidationFacade;
    private final VoteValidationCache voteValidationCache;

    public VoteResponse registerVote(VoteRequest request) {
        log.debug("vote - session={}, cpf={}", request.sessionId(), request.cpf());

        SessionCache sessionCache = sessionValidationCache.get(request.sessionId());
        if (sessionCache.closedOrExpired(Instant.now())) {
            return VoteResponse.invalidSession();
        }

        String cpf = formatCpf(request.cpf());

        VoteResponse validationResult = validateCpf(cpf);
        if (validationResult != null) {
            return validationResult;
        }

        VoteResponse duplicateVoteCheck = duplicateVoteCheck(request.sessionId(), cpf, sessionCache);
        if (duplicateVoteCheck != null) {
            return duplicateVoteCheck;
        }

        return publishVote(request.sessionId(), cpf, request.vote());
    }

    private VoteResponse validateCpf(String cpf) {
        CpfValidationResponse response = cpfValidationFacade.validateCpf(cpf);
        if (response == null) return VoteResponse.invalidCpf();
        if (response.status() == VoteStatus.UNABLE_TO_VOTE) return VoteResponse.unableToVote();
        return null; // válido
    }

    private VoteResponse duplicateVoteCheck(Long sessionId, String cpf, SessionCache sessionCache) {
        Duration ttl = calculateTtl(sessionCache);
        if (!voteValidationCache.reserve(sessionId, cpf, ttl)) {
            return VoteResponse.alreadyVoted();
        }
        return null;
    }

    private Duration calculateTtl(SessionCache sessionCache) {
        Duration ttl = Duration.between(Instant.now(), sessionCache.expiresAt()).plusHours(1);
        return ttl.isNegative() || ttl.isZero() ? Duration.ofHours(1) : ttl;
    }

    private VoteResponse publishVote(Long sessionId, String cpf, VoteType vote) {
        VoteMessage message = VoteMessage.builder()
                .sessionId(sessionId)
                .cpf(cpf)
                .vote(vote)
                .build();
        try {
            voteProducer.sendVote(message);
            return VoteResponse.ok();
        } catch (Exception ex) {
            voteValidationCache.release(sessionId, cpf);
            throw ex;
        }
    }
}
