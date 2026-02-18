package com.test.voting.messaging;

import com.test.voting.dto.VoteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteRoutingPublisher {

    private static final int MAX_RETRIES = 3;
    private final RabbitTemplate rabbitTemplate;

    public void publishToMain(VoteMessage voteMessage) {
        log.debug("Publishing to main queue - Session: {}, CPF: {}",
                voteMessage.getSessionId(), voteMessage.getCpf());
        rabbitTemplate.convertAndSend(RabbitNames.EXCHANGE, RabbitNames.RK_CREATE, voteMessage);
    }

    public void routeRetryOrDlq(Message message, Exception ex) {
        int retryCount = getRetryCount(message);
        if (retryCount < MAX_RETRIES) {
            publishToRetry(message, ex);
        } else {
            publishToDlq(message, ex);
        }
    }

    private void publishToRetry(Message message, Exception ex) {
        int retryCount = incrementRetryCount(message);
        addErrorHeaders(message, ex);
        log.info("Publishing to retry queue - Attempt: {}/{}", retryCount, MAX_RETRIES);
        rabbitTemplate.send(RabbitNames.EXCHANGE, RabbitNames.RK_RETRY, message);
    }

    private void publishToDlq(Message message, Exception ex) {
        addErrorHeaders(message, ex);
        log.error("Max retries exceeded, publishing to DLQ - Error: {}", ex.getMessage());
        rabbitTemplate.send(RabbitNames.EXCHANGE, RabbitNames.RK_DLQ, message);
    }

    private int getRetryCount(Message message) {
        Object header = message.getMessageProperties().getHeader(RabbitNames.HEADER_RETRY_COUNT);
        return header != null ? (int) header : 0;
    }

    private int incrementRetryCount(Message message) {
        MessageProperties props = message.getMessageProperties();
        int retryCount = getRetryCount(message) + 1;
        props.setHeader(RabbitNames.HEADER_RETRY_COUNT, retryCount);
        return retryCount;
    }

    private void addErrorHeaders(Message message, Exception ex) {
        MessageProperties props = message.getMessageProperties();
        props.setHeader(RabbitNames.HEADER_ERROR_TYPE, ex.getClass().getSimpleName());
        props.setHeader(RabbitNames.HEADER_ERROR_MESSAGE, ex.getMessage());
    }
}
