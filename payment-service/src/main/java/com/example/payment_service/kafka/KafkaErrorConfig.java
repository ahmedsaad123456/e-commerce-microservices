package com.example.payment_service.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        // After all retries failed: publish the SAME data to "<original topic>.DLT".
        // Partition -1 = "let Kafka choose by key" (so the DLT can have fewer partitions than the original).
        // DLT: Dead Letter Topic
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (data, ex) -> new TopicPartition(data.topic() + ".DLT", -1));

        // 1 first attempt + 3 retries, waiting 1s, 2s, 4s between them
        var backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Retrying cannot fix a message that is not valid JSON, so skip the retries and go straight to the DLT
        handler.addNotRetryableExceptions(JacksonException.class, IllegalArgumentException.class);
        return handler;
    }

    @Bean
    NewTopic orderTopicDlt() {
        return TopicBuilder.name("order-topic.DLT").partitions(1).replicas(1).build();
    }
}