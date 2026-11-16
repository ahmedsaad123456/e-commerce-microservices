package com.example.order_service.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RabbitEmailPublisher {

    private final RabbitTemplate rabbit;

    public void publish(String exchange, String routingKey, UUID messageId, String jsonPayload) {
        // The JSON is already built (it comes from the outbox), so we send the bytes as they are.
        // That is why no JSON converter is needed on the sending side.
        var message = MessageBuilder.withBody(jsonPayload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(messageId.toString())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)          // written to disk by the broker
                .build();

        // send() alone returns as soon as the bytes leave our machine. Waiting for the broker's confirmation makes
        // "publish returned" mean "RabbitMQ really has it". If it does not confirm in 5s, this throws.
        rabbit.invoke(ops -> {
            ops.send(exchange, routingKey, message);
            ops.waitForConfirmsOrDie(5_000);
            return true;
        });
    }
}
