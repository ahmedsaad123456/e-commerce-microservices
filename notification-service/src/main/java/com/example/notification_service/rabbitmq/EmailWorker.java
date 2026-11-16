package com.example.notification_service.rabbitmq;

import com.example.notification_service.entity.Notification;
import com.example.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import com.rabbitmq.client.Channel;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailWorker {

    // Maximum number of processing attempts before sending the message to the DLQ.
    private static final int MAX_ATTEMPTS = 3;

    private final JsonMapper json;
    private final NotificationService notifications;
    private final RabbitTemplate rabbit;

    // Optional delay used to simulate slow email processing.
    @Value("${notification.worker.simulated-delay-ms:0}")
    private long simulatedDelayMs;

    // Identifies the current worker instance in the logs.
    private final String instance = System.getenv().getOrDefault("HOSTNAME", "local");

    // Receive the raw message so even malformed JSON can be handled safely.
    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void handle(Message message, Channel channel) throws IOException {

        // Get the delivery tag required to acknowledge or reject this message.
        long tag = message.getMessageProperties().getDeliveryTag();

        // Calculate the current attempt using RabbitMQ's dead-letter metadata.
        int attempt = failedAttempts(message) + 1;

        SendEmailJob job = null;

        try {
            // Deserialize the message body into an email job.
            job = json.readValue(message.getBody(), SendEmailJob.class);

            log.info("[{}] email {} for order {} (attempt {}/{})",
                    instance, job.type(), job.orderReference(), attempt, MAX_ATTEMPTS);

            // Simulate processing time when configured.
            pause(simulatedDelayMs);

            // Record the notification; an existing eventId returns the existing notification.
            // This prevents duplicate notification records when RabbitMQ redelivers a message.
            var notification = notifications.saveNotification(Notification.queued(
                    job.eventId().toString(),
                    job.type(),
                    job.customerId(),
                    job.orderReference(),
                    job.amount(),
                    job.detail()
            ));

            // Send the email only if the notification has not already been sent.
            notifications.deliver(notification.getId());

            // Acknowledge the message only after successful processing.
            channel.basicAck(tag, false);

        } catch (JacksonException | IllegalArgumentException permanent) {

            // Malformed messages or permanent business errors should not be retried.
            giveUp(message, channel, tag, job, permanent);

        } catch (Exception e) {

            // Move the message to the DLQ after reaching the maximum number of attempts.
            if (attempt >= MAX_ATTEMPTS) {
                giveUp(message, channel, tag, job, e);
            } else {
                log.warn("[{}] attempt {} failed: {}. It will be retried after a delay",
                        instance, attempt, e.toString());

                // Reject without requeueing; the queue's DLX routes it to the retry queue.
                channel.basicNack(tag, false, false);
            }
        }
    }

    // Send permanently failed messages to the Dead Letter Queue.
    private void giveUp(
            Message message,
            Channel channel,
            long tag,
            SendEmailJob job,
            Exception cause
    ) throws IOException {

        log.error("[{}] giving up: {}", instance, cause.toString());

        // Store the final error in the message headers for later inspection.
        message.getMessageProperties().setHeader("x-final-error", cause.toString());

        // Publish the failed message directly to the Dead Letter Exchange.
        rabbit.send(RabbitConfig.DLX, RabbitConfig.DEAD_KEY, message);

        if (job != null) {
            // Mark the notification as failed when the job was successfully parsed.
            notifications.markFailed(job.eventId().toString(), cause.toString());
        }

        // Acknowledge the original message after publishing it to the DLX.
        channel.basicAck(tag, false);
    }

    // Read RabbitMQ's x-death header to determine how many times this message was rejected.
    private int failedAttempts(Message message) {

        var xDeath = message.getMessageProperties().getXDeathHeader();

        // A message without x-death metadata has not been retried yet.
        if (xDeath == null) return 0;

        // Find the rejection count for the main email queue.
        return xDeath.stream()
                .filter(d -> RabbitConfig.EMAIL_QUEUE.equals(d.get("queue"))
                        && "rejected".equals(d.get("reason")))
                .map(d -> ((Number) d.get("count")).intValue())
                .findFirst()
                .orElse(0);
    }

    // Pause the current worker for the configured duration.
    private void pause(long ms) {
        if (ms <= 0) return;

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Restore the interrupted status instead of silently swallowing the interruption.
            Thread.currentThread().interrupt();
        }
    }
}