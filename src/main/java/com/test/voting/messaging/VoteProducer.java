package com.test.voting.messaging;

import com.test.voting.dto.VoteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteProducer {

    private final VoteRoutingPublisher routingPublisher;

    public void sendVote(VoteMessage message) {
        log.debug("Sending vote - Session: {}, CPF: {}", message.getSessionId(), message.getCpf());
        routingPublisher.publishToMain(message);
    }
}
