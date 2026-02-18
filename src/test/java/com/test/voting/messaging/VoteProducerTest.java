package com.test.voting.messaging;

import com.test.voting.dto.VoteMessage;
import com.test.voting.model.enums.VoteType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteProducerTest {

    @Mock
    private VoteRoutingPublisher routingPublisher;

    @InjectMocks
    private VoteProducer voteProducer;

    @Test
    void shouldDelegateToRoutingPublisher() {
        VoteMessage message = VoteMessage.builder()
                .sessionId(1L).cpf("12345678909").vote(VoteType.YES).build();

        voteProducer.sendVote(message);

        verify(routingPublisher).publishToMain(message);
    }
}
