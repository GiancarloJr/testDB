package com.test.voting.scheduler;

import com.test.voting.model.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionSchedulerService {

    private final Scheduler scheduler;
    private final Clock clock;

    private final static Integer RETRY_COUNT = 3;
    private final static Integer INTERVAL_MINUTES = 1;
    private final static String SESSION_ID_KEY = "sessionId";
    private final static String RETRY_COUNT_KEY = "retryCount";

    public void scheduleSessionClosure(Session session) {
        try {
            String jobName = "close-session-" + session.getId();
            String jobDescription = "Close session " + session.getId();
            String jobGroup = "session-jobs";

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(SESSION_ID_KEY, session.getId());
            jobDataMap.put(RETRY_COUNT_KEY, 0);

            JobDetail jobDetail = buildJobDetail(jobName, jobGroup, jobDescription, jobDataMap, CloseSessionJob.class);

            Date executionTime = Date.from(
                    session.getExpirationTime()
                            .atZone(clock.getZone())
                            .toInstant()
            );

            SimpleScheduleBuilder scheduleBuilder = buildSimpleScheduleBuilderByMinute(RETRY_COUNT, INTERVAL_MINUTES);

            Trigger trigger = buildTriggerConfig(jobName,
                    jobGroup,
                    executionTime,
                    scheduleBuilder);

            scheduler.scheduleJob(jobDetail, trigger);

            log.info("Scheduled automatic closure for session {} at {}",
                    session.getId(), session.getExpirationTime());

        } catch (SchedulerException e) {
            log.error("Error scheduling session closure for session {}: {}",
                    session.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to schedule session closure", e);
        }
    }

    private Trigger buildTriggerConfig(String jobName, String jobGroup, Date executionTime, SimpleScheduleBuilder scheduleBuilder) {
        return TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + jobName, jobGroup)
                .startAt(executionTime)
                .withSchedule(scheduleBuilder)
                .build();
    }

    private SimpleScheduleBuilder buildSimpleScheduleBuilderByMinute(Integer retryCount, Integer intervalMinutes) {
        return SimpleScheduleBuilder.simpleSchedule()
                .withMisfireHandlingInstructionFireNow()
                .withRepeatCount(retryCount)
                .withIntervalInMinutes(intervalMinutes);
    }

    private JobDetail buildJobDetail(String jobName, String jobGroup, String jobDescription, JobDataMap jobDataMap, Class<? extends Job> jobClass) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .withDescription(jobDescription)
                .usingJobData(jobDataMap)
                .storeDurably(true)
                .requestRecovery(true)
                .build();
    }
}
