package com.test.voting.messaging;

import com.test.voting.dto.VoteMessage;
import com.test.voting.model.enums.VoteType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteRoutingPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private VoteRoutingPublisher publisher;

    @Test
    void shouldPublishToMainQueue() {
        VoteMessage voteMessage = VoteMessage.builder()
                .sessionId(1L).cpf("12345678909").vote(VoteType.YES).build();

        publisher.publishToMain(voteMessage);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitNames.EXCHANGE), eq(RabbitNames.RK_CREATE), eq(voteMessage));
    }

    @Test
    void shouldRouteToRetryWhenRetryCountBelowMax() {
        Message message = new Message("test".getBytes(), new MessageProperties());
        Exception ex = new RuntimeException("DB error");

        publisher.routeRetryOrDlq(message, ex);

        verify(rabbitTemplate).send(eq(RabbitNames.EXCHANGE), eq(RabbitNames.RK_RETRY), eq(message));
        verify(rabbitTemplate, never()).send(eq(RabbitNames.EXCHANGE), eq(RabbitNames.RK_DLQ), any());
    }

    @Test
    void shouldRouteToDlqWhenMaxRetriesExceeded() {
        MessageProperties props = new MessageProperties();
        props.setHeader(RabbitNames.HEADER_RETRY_COUNT, 3);
        Message message = new Message("test".getBytes(), props);
        Exception ex = new RuntimeException("DB error");

        publisher.routeRetryOrDlq(message, ex);

        verify(rabbitTemplate).send(eq(RabbitNames.EXCHANGE), eq(RabbitNames.RK_DLQ), eq(message));
        verify(rabbitTemplate, never()).send(eq(RabbitNames.EXCHANGE), eq(RabbitNames.RK_RETRY), any());
    }

    @Test
    void shouldIncrementRetryCountOnEachRetry() {
        Message message = new Message("test".getBytes(), new MessageProperties());
        Exception ex = new RuntimeException("error");

        publisher.routeRetryOrDlq(message, ex);

        Integer retryCount = message.getMessageProperties().getHeader(RabbitNames.HEADER_RETRY_COUNT);
        assertRetryCount(retryCount, 1);
    }

    @Test
    void shouldAddErrorHeadersOnRetry() {
        Message message = new Message("test".getBytes(), new MessageProperties());
        RuntimeException ex = new RuntimeException("connection lost");

        publisher.routeRetryOrDlq(message, ex);

        MessageProperties props = message.getMessageProperties();
        assertErrorHeaders(props, "RuntimeException", "connection lost");
    }

    private void assertRetryCount(Integer actual, int expected) {
        assert actual != null && actual == expected :
                "Expected retry count " + expected + " but was " + actual;
    }

    private void assertErrorHeaders(MessageProperties props, String expectedType, String expectedMsg) {
        String errorType = props.getHeader(RabbitNames.HEADER_ERROR_TYPE);
        String errorMessage = props.getHeader(RabbitNames.HEADER_ERROR_MESSAGE);
        assert expectedType.equals(errorType) : "Expected error type " + expectedType + " but was " + errorType;
        assert expectedMsg.equals(errorMessage) : "Expected error message " + expectedMsg + " but was " + errorMessage;
    }
}
