package com.test.voting.messaging;

import com.rabbitmq.client.Channel;
import com.test.voting.dto.VoteMessage;
import com.test.voting.model.enums.VoteType;
import com.test.voting.repository.VoteBatchInserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteConsumerTest {

    @Mock
    private VoteRoutingPublisher routingPublisher;
    @Mock
    private MessageConverter jsonMessageConverter;
    @Mock
    private VoteBatchInserter voteBatchInserter;
    @Mock
    private Channel channel;

    @InjectMocks
    private VoteConsumer voteConsumer;

    private VoteMessage voteMessage;
    private Message msg;

    @BeforeEach
    void setUp() {
        msg = createMessage(10L);
        voteMessage = VoteMessage.builder()
                .sessionId(1L).cpf("12345678909").vote(VoteType.YES).build();
        when(jsonMessageConverter.fromMessage(msg)).thenReturn(voteMessage);
    }

    private Message createMessage(long deliveryTag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(deliveryTag);
        return new Message("{}".getBytes(), props);
    }

    @Test
    void shouldAckAndInsertBatchOnSuccess() throws IOException {

        when(jsonMessageConverter.fromMessage(msg)).thenReturn(voteMessage);

        voteConsumer.consume(List.of(msg), channel);

        verify(voteBatchInserter).insertBatch(anyList());
        verify(channel).basicAck(10L, true);
        verifyNoInteractions(routingPublisher);
    }

    @Test
    void shouldRouteToRetryOnBatchFailure() throws IOException {
        when(jsonMessageConverter.fromMessage(msg)).thenReturn(voteMessage);
        doThrow(new RuntimeException("DB error")).when(voteBatchInserter).insertBatch(anyList());

        voteConsumer.consume(List.of(msg), channel);

        verify(channel).basicAck(10L, true);
        verify(routingPublisher).routeRetryOrDlq(eq(msg), any(RuntimeException.class));
    }
}
