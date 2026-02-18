package com.test.voting.scheduler;

import com.test.voting.model.Session;
import com.test.voting.model.enums.SessionStatus;
import com.test.voting.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloseSessionJobTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private JobExecutionContext context;
    @Mock
    private JobDetail jobDetail;

    @InjectMocks
    private CloseSessionJob closeSessionJob;

    private static final String SESSION_ID_KEY = "sessionId";

    @BeforeEach
    void setUp() {
        JobDataMap dataMap = new JobDataMap();
        jobDetail = JobBuilder.newJob(CloseSessionJob.class)
                .withIdentity("closeSessionJob", "test")
                .usingJobData(dataMap)
                .build();

        when(context.getJobDetail()).thenReturn(jobDetail);
    }

    @Test
    void shouldCloseOpenSession_andPersist() throws Exception {
        withSessionId(1L);

        Session session = Session.builder()
                .id(1L)
                .status(SessionStatus.OPEN)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        closeSessionJob.execute(context);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSE);

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(SessionStatus.CLOSE);

        verify(sessionRepository).findById(1L);
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void shouldSkipAlreadyClosedSession_andNotPersist() throws Exception {
        withSessionId(1L);

        Session session = Session.builder()
                .id(1L)
                .status(SessionStatus.CLOSE)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        closeSessionJob.execute(context);

        verify(sessionRepository, never()).save(any());
        verify(sessionRepository).findById(1L);
        verifyNoMoreInteractions(sessionRepository);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSE);
    }

    @Test
    void shouldWrapNotFoundIntoJobExecutionException() {
        withSessionId(99L);

        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> closeSessionJob.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        verify(sessionRepository).findById(99L);
        verify(sessionRepository, never()).save(any());
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void shouldWrapAnyRepositoryFailureIntoJobExecutionException() {
        withSessionId(1L);

        when(sessionRepository.findById(1L)).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> closeSessionJob.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);

        verify(sessionRepository).findById(1L);
        verify(sessionRepository, never()).save(any());
        verifyNoMoreInteractions(sessionRepository);
    }

    private void withSessionId(long sessionId) {
        context.getJobDetail().getJobDataMap().put(SESSION_ID_KEY, sessionId);
    }
}
