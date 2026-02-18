package com.test.voting.service;

import com.test.voting.cache.SessionValidationCache;
import com.test.voting.cache.VoteValidationCache;
import com.test.voting.dto.CpfValidationResponse;
import com.test.voting.dto.SessionCache;
import com.test.voting.dto.VoteMessage;
import com.test.voting.dto.VoteRequest;
import com.test.voting.dto.VoteResponse;
import com.test.voting.facade.CpfValidationFacade;
import com.test.voting.messaging.VoteProducer;
import com.test.voting.model.enums.SessionStatus;
import com.test.voting.model.enums.VoteStatus;
import com.test.voting.model.enums.VoteType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteProducer voteProducer;
    @Mock
    private SessionValidationCache sessionValidationCache;
    @Mock
    private CpfValidationFacade cpfValidationFacade;
    @Mock
    private VoteValidationCache voteValidationCache;

    @InjectMocks
    private VoteService voteService;

    private static final String VALID_CPF = "01582728119";
    private static final Long SESSION_ID = 1L;

    @Test
    void shouldReturnOkWhenVoteIsSuccessful() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(openSession());
        when(cpfValidationFacade.validateCpf(VALID_CPF))
                .thenReturn(new CpfValidationResponse(VoteStatus.ABLE_TO_VOTE));
        when(voteValidationCache.reserve(eq(SESSION_ID), eq(VALID_CPF), any(Duration.class)))
                .thenReturn(true);

        VoteResponse response = voteService.registerVote(buildRequest());

        assertThat(response.httpStatus()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.message()).isEqualTo("Vote registered successfully.");
        verify(voteProducer).sendVote(any(VoteMessage.class));
    }

    @Test
    void shouldReturnInvalidSessionWhenExpired() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(expiredSession());

        VoteResponse response = voteService.registerVote(buildRequest());

        assertThat(response.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(cpfValidationFacade, voteProducer);
    }

    @Test
    void shouldReturnInvalidSessionWhenClosed() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(closedSession());

        VoteResponse response = voteService.registerVote(buildRequest());

        assertThat(response.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(cpfValidationFacade, voteProducer);
    }

    @Test
    void shouldReturnInvalidCpfWhenFacadeReturnsNull() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(openSession());
        when(cpfValidationFacade.validateCpf(VALID_CPF)).thenReturn(null);

        VoteResponse response = voteService.registerVote(buildRequest());

        assertThat(response.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.message()).isEqualTo("Invalid cpf");
        verifyNoInteractions(voteProducer);
    }

    @Test
    void shouldReturnUnableToVoteWhenCpfIsUnable() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(openSession());
        when(cpfValidationFacade.validateCpf(VALID_CPF))
                .thenReturn(new CpfValidationResponse(VoteStatus.UNABLE_TO_VOTE));

        VoteResponse response = voteService.registerVote(buildRequest());

        assertThat(response.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.status()).isEqualTo(VoteStatus.UNABLE_TO_VOTE);
        verifyNoInteractions(voteProducer);
    }

    @Test
    void shouldReturnAlreadyVotedWhenReserveFails() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(openSession());
        when(cpfValidationFacade.validateCpf(VALID_CPF))
                .thenReturn(new CpfValidationResponse(VoteStatus.ABLE_TO_VOTE));
        when(voteValidationCache.reserve(eq(SESSION_ID), eq(VALID_CPF), any(Duration.class)))
                .thenReturn(false);

        VoteResponse response = voteService.registerVote(buildRequest());

        assertThat(response.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(voteProducer);
    }

    @Test
    void shouldReleaseReservationWhenProducerFails() {
        when(sessionValidationCache.get(SESSION_ID)).thenReturn(openSession());
        when(cpfValidationFacade.validateCpf(VALID_CPF))
                .thenReturn(new CpfValidationResponse(VoteStatus.ABLE_TO_VOTE));
        when(voteValidationCache.reserve(eq(SESSION_ID), eq(VALID_CPF), any(Duration.class)))
                .thenReturn(true);

        doThrow(new RuntimeException()).when(voteProducer).sendVote(any());
        assertThatThrownBy(() -> voteService.registerVote(buildRequest()))
                .isInstanceOf(RuntimeException.class);

        verify(voteValidationCache).release(SESSION_ID, VALID_CPF);
    }

    private VoteRequest buildRequest() {
        return new VoteRequest(SESSION_ID, VALID_CPF, VoteType.YES);
    }

    private SessionCache openSession() {
        return new SessionCache(SessionStatus.OPEN, Instant.now().plusSeconds(3600));
    }

    private SessionCache expiredSession() {
        return new SessionCache(SessionStatus.OPEN, Instant.now().minusSeconds(60));
    }

    private SessionCache closedSession() {
        return new SessionCache(SessionStatus.CLOSE, Instant.now().plusSeconds(3600));
    }
}
