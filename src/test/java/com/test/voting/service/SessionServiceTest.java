package com.test.voting.service;

import com.test.voting.dto.ResultResponse;
import com.test.voting.dto.SessionRequest;
import com.test.voting.dto.SessionResponse;
import com.test.voting.exception.ResourceNotFoundException;
import com.test.voting.mapper.SessionMapper;
import com.test.voting.model.Session;
import com.test.voting.model.enums.SessionStatus;
import com.test.voting.repository.SessionRepository;
import com.test.voting.repository.VoteRepository;
import com.test.voting.scheduler.SessionSchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private SessionSchedulerService sessionSchedulerService;
    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private SessionService sessionService;

    private static final String SESSION_DESCRIPTION = "SESSION 1";
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(2026, 2, 17, 12, 0);
    private static final LocalDateTime SESSION_END_TIME = LocalDateTime.of(2026, 2, 17, 12, 5);

    @Test
    void shouldCreateSessionSuccessfully() {
        SessionRequest request = new SessionRequest(SESSION_DESCRIPTION, 5);

        Session mappedEntity = Session.builder().description(SESSION_DESCRIPTION).build();
        Session savedEntity = Session.builder().id(1L).description(SESSION_DESCRIPTION).build();

        SessionResponse expected = new SessionResponse(
                1L,
                SESSION_START_TIME,
                SESSION_END_TIME,
                SessionStatus.OPEN
        );

        when(sessionMapper.toEntity(request)).thenReturn(mappedEntity);
        when(sessionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(sessionMapper.toResponse(savedEntity)).thenReturn(expected);

        SessionResponse result = sessionService.create(request);

        assertThat(result).isEqualTo(expected);

        InOrder inOrder = inOrder(sessionMapper, sessionRepository, sessionSchedulerService);

        inOrder.verify(sessionMapper).toEntity(request);
        inOrder.verify(sessionRepository).save(mappedEntity);
        inOrder.verify(sessionSchedulerService).scheduleSessionClosure(savedEntity);
        inOrder.verify(sessionMapper).toResponse(savedEntity);

        verifyNoMoreInteractions(sessionMapper, sessionRepository, sessionSchedulerService);
    }

    @Test
    void shouldNotScheduleWhenSaveFails() {
        SessionRequest request = new SessionRequest(SESSION_DESCRIPTION, 5);
        Session mappedEntity = Session.builder().description(SESSION_DESCRIPTION).build();

        when(sessionMapper.toEntity(request)).thenReturn(mappedEntity);
        when(sessionRepository.save(mappedEntity)).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> sessionService.create(request))
                .isInstanceOf(RuntimeException.class);

        verify(sessionSchedulerService, never()).scheduleSessionClosure(any());
        verify(sessionMapper, never()).toResponse(any());
    }

    @Test
    void shouldFindSessionById() {
        Session session = Session.builder().id(1L).description(SESSION_DESCRIPTION).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        Session result = sessionService.findById(1L);

        assertThat(result).isSameAs(session);
        verify(sessionRepository).findById(1L);
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void shouldThrowWhenSessionNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sessionRepository).findById(99L);
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void shouldReturnResultForOpenSession() {
        Session session = Session.builder()
                .id(1L)
                .status(SessionStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(voteRepository.countVotesBySessionId(1L))
                .thenReturn(new ResultResponse.VoteCount(10L, 5L, 15L));

        ResultResponse result = sessionService.getResult(1L);

        assertThat(result.voteCount()).isNotNull();
        assertThat(result.voteCount().total()).isEqualTo(15L);
        assertThat(result.voteCount().yes()).isEqualTo(10L);
        assertThat(result.voteCount().no()).isEqualTo(5L);
    }

    @Test
    void shouldReturnResultForClosedSession() {
        Session session = Session.builder()
                .id(1L)
                .status(SessionStatus.CLOSE)
                .createdAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(voteRepository.countVotesBySessionId(1L))
                .thenReturn(new ResultResponse.VoteCount(0L, 0L, 0L));

        ResultResponse result = sessionService.getResult(1L);

        assertThat(result.sessionId()).isEqualTo(1L);
        assertThat(result.sessionStatus()).isEqualTo(SessionStatus.CLOSE);
    }
}
