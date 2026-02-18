package com.test.voting.config;

import com.test.voting.messaging.RabbitNames;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setPrefetchCount(100);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setReceiveTimeout(500L);
        return factory;
    }

    @Bean
    public DirectExchange votesExchange() {
        return new DirectExchange(RabbitNames.EXCHANGE, true, false);
    }

    @Bean
    public Queue votesQueue() {
        return QueueBuilder.durable(RabbitNames.QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", RabbitNames.EXCHANGE,
                        "x-dead-letter-routing-key", RabbitNames.RK_DLQ
                ))
                .build();
    }

    @Bean
    public Queue votesRetryQueue() {
        return QueueBuilder.durable(RabbitNames.QUEUE_RETRY)
                .withArguments(Map.of(
                        "x-message-ttl", 60_000,
                        "x-dead-letter-exchange", RabbitNames.EXCHANGE,
                        "x-dead-letter-routing-key", RabbitNames.RK_CREATE
                ))
                .build();
    }

    @Bean
    public Queue votesDlqQueue() {
        return QueueBuilder.durable(RabbitNames.QUEUE_DLQ).build();
    }

    @Bean
    public Binding votesBinding() {
        return BindingBuilder.bind(votesQueue()).to(votesExchange()).with(RabbitNames.RK_CREATE);
    }

    @Bean
    public Binding retryBinding() {
        return BindingBuilder.bind(votesRetryQueue()).to(votesExchange()).with(RabbitNames.RK_RETRY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(votesDlqQueue()).to(votesExchange()).with(RabbitNames.RK_DLQ);
    }
}
