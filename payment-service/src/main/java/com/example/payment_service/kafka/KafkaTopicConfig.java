package com.example.payment_service.kafka;

import com.example.payment_service.common.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {


    @Bean
    NewTopic orderTopic() {
        // 3 partitions = up to 3 consumers of the same group can work in parallel
        return TopicBuilder.name(Constants.PAYMENT_TOPIC).partitions(3).replicas(1).build();
    }
}