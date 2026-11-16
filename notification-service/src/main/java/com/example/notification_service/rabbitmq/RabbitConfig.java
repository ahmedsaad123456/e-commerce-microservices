
package com.example.notification_service.rabbitmq;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Main exchange and queue for incoming email messages.
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_QUEUE = "email.queue";

    // Retry exchange and queue used to delay failed messages before retrying.
    public static final String RETRY_EXCHANGE = "email.retry.exchange";
    public static final String RETRY_QUEUE = "email.retry.queue";

    // Dead Letter Exchange and Queue for messages that fail permanently.
    public static final String DLX = "email.dlx";
    public static final String DLQ = "email.dlq";

    // Routing keys determine which queue receives each message.
    public static final String DEAD_KEY = "email.dead";
    public static final String KEY_ORDER = "email.order";
    public static final String KEY_PAYMENT = "email.payment";

    // Failed messages wait 10 seconds in the retry queue before being routed back.
    private static final int RETRY_DELAY_MS = 10_000;

    // RabbitAdmin automatically declares these Exchange, Queue, and Binding beans
    // on the RabbitMQ broker when the application connects.

    // Main direct exchange routes messages using exact routing-key matches.
    // Exchanges are durable by default.
    @Bean
    DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    // Retry direct exchange routes failed messages to the retry queue.
    @Bean
    DirectExchange retryExchange() {
        return new DirectExchange(RETRY_EXCHANGE);
    }

    // Dead Letter Exchange routes permanently failed messages to the DLQ.
    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)

                // When a message is rejected without requeueing,
                // RabbitMQ forwards it to the configured retry exchange.
                // The original routing key is preserved because no DL routing key is specified.
                .deadLetterExchange(RETRY_EXCHANGE)

                .build();
    }

    @Bean
    Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)

                // Messages expire after 10 seconds because of the queue TTL.
                // No consumer is attached to this queue; it acts as a delay queue.
                .ttl(RETRY_DELAY_MS)

                // After expiration, RabbitMQ forwards messages back to the main exchange.
                .deadLetterExchange(EMAIL_EXCHANGE)

                // The original routing key is preserved to route the message
                // back to the appropriate main queue binding.
                .build();
    }

    @Bean
    Queue deadLetterQueue() {

        // Stores messages that have exhausted their retries or cannot be processed.
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Declarables bindings() {

        // Declares all exchange-to-queue bindings in one configuration bean.
        return new Declarables(

                // Route order notification messages to the main email queue.
                BindingBuilder.bind(emailQueue())
                        .to(emailExchange())
                        .with(KEY_ORDER),

                // Route payment notification messages to the main email queue.
                BindingBuilder.bind(emailQueue())
                        .to(emailExchange())
                        .with(KEY_PAYMENT),

                // Route failed order messages to the retry queue.
                BindingBuilder.bind(retryQueue())
                        .to(retryExchange())
                        .with(KEY_ORDER),

                // Route failed payment messages to the retry queue.
                BindingBuilder.bind(retryQueue())
                        .to(retryExchange())
                        .with(KEY_PAYMENT),

                // Route permanently failed messages to the Dead Letter Queue.
                BindingBuilder.bind(deadLetterQueue())
                        .to(deadLetterExchange())
                        .with(DEAD_KEY)
        );
    }
}
