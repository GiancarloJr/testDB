package com.test.voting.messaging;

public final class RabbitNames {
    private RabbitNames() {}

    public static final String EXCHANGE    = "votes.exchange";
    public static final String QUEUE       = "votes.queue";
    public static final String QUEUE_RETRY = "votes.queue.retry";
    public static final String QUEUE_DLQ   = "votes.queue.dlq";

    public static final String RK_CREATE   = "votes.create";
    public static final String RK_RETRY    = "votes.create.retry";
    public static final String RK_DLQ      = "votes.create.dlq";

    public static final String HEADER_RETRY_COUNT   = "x-retry-count";
    public static final String HEADER_ERROR_TYPE     = "x-error-type";
    public static final String HEADER_ERROR_MESSAGE  = "x-error-message";
}
