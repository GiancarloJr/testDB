package com.test.voting.messaging;

import com.rabbitmq.client.Channel;
import com.test.voting.dto.VoteMessage;
import com.test.voting.repository.VoteBatchInserter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteConsumer {

    private final VoteRoutingPublisher routingPublisher;
    private final MessageConverter jsonMessageConverter;
    private final VoteBatchInserter voteBatchInserter;

    @RabbitListener(
            queues = RabbitNames.QUEUE,
            containerFactory = "rabbitListenerContainerFactory",
            ackMode = "MANUAL"
    )
    public void consume(List<Message> messages, Channel channel) throws IOException {
        long lastTag = messages.getLast()
                .getMessageProperties().getDeliveryTag();

        List<VoteMessage> batchMessages = new ArrayList<>(messages.size());
        for (Message m : messages) {
            batchMessages.add((VoteMessage) jsonMessageConverter.fromMessage(m));
        }

        log.debug("Consuming votes - Session: {}", batchMessages.getFirst().getSessionId());

        try {
            voteBatchInserter.insertBatch(batchMessages);
            channel.basicAck(lastTag, true);
        } catch (Exception ex) {
            log.error("Batch failed ({} votes), routing to retry: {}", messages.size(), ex.getMessage());
            channel.basicAck(lastTag, true);
            for (Message m : messages) {
                routingPublisher.routeRetryOrDlq(m, ex);
            }
        }
    }
}
