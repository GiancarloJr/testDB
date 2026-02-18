package com.test.voting.scheduler;

import com.test.voting.model.enums.SessionStatus;
import com.test.voting.exception.ResourceNotFoundException;
import com.test.voting.model.Session;
import com.test.voting.repository.SessionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@AllArgsConstructor
public class CloseSessionJob implements Job {

    private final SessionRepository sessionRepository;

    private static final String SESSION_ID_KEY = "sessionId";

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long sessionId = null;

        try {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();

            long rawId = dataMap.getLong(SESSION_ID_KEY);

            if (rawId <= 0) {
                throw new IllegalArgumentException("Missing/invalid sessionId in JobDataMap");
            }
            sessionId = rawId;

            log.info("Executing scheduled job to close session: {}", sessionId);

            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

            closeSession(session);

        } catch (Exception e) {
            log.error("Error closing session {}: {}", sessionId, e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

    private void closeSession(Session session) {
        if (session.getStatus() == SessionStatus.OPEN) {

            session.setStatus(SessionStatus.CLOSE);
            sessionRepository.save(session);

            log.info("Session {} automatically closed at expiration time.", session.getId());
        } else {
            log.info("Session {} already closed, skipping", session.getId());
        }
    }
}
