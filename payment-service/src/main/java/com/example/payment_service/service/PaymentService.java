package com.example.payment_service.service;

import com.example.payment_service.common.Constants;
import com.example.payment_service.dto.PaymentView;
import com.example.payment_service.entity.OrderConfirmation;
import com.example.payment_service.entity.PaymentResult;
import com.example.payment_service.entity.PaymentStatus;
import com.example.payment_service.exceptions.ResourceNotFoundException;
import com.example.payment_service.mapper.PaymentMapper;
import com.example.payment_service.outbox.OutboxEvent;
import com.example.payment_service.payment_gateway.PaymentGateway;
import com.example.payment_service.rabbitmq.NotificationType;
import com.example.payment_service.rabbitmq.RabbitTopology;
import com.example.payment_service.rabbitmq.SendEmailJob;
import com.example.payment_service.repository.OutboxRepository;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {


    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final OutboxRepository outbox;
    private final JsonMapper json;
    private final PaymentMapper mapper;

    @Transactional
    public void process(OrderConfirmation order) {
        // 1) CLAIM first. A duplicate event stops here, BEFORE we charge anything.
        if (payments.claim(UUID.randomUUID(), order.orderReference(), order.customerId(), order.totalAmount()) == 0) {
            log.info("Order {} was already processed, ignoring the duplicate event", order.orderReference());
            return;
        }

        // 2) Charge. A decline is a normal business RESULT (status FAILED), not an exception.
        //    A technical failure (timeout) IS an exception: the whole transaction rolls back, the claim
        //    disappears, and Kafka redelivers the event (see KafkaErrorConfig).
        var charge = gateway.charge(order.orderReference(), order.totalAmount());
        PaymentStatus status = charge.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;

        // 3) Record the outcome and the outbox event in the SAME transaction
        payments.findByOrderReference(order.orderReference()).orElseThrow().finish(status, charge.reason());

        var emailId = UUID.randomUUID();
        var emailType = status == PaymentStatus.SUCCEEDED ? NotificationType.PAYMENT_SUCCEEDED : NotificationType.PAYMENT_FAILED;
        var email = new SendEmailJob(emailId, emailType, order.customerId(), order.orderReference(),
                order.totalAmount(), charge.reason());
        // 1) Kafka: the payment RESULT that order-service listens to on payment-topic
        var resultId = UUID.randomUUID();
        var result = new PaymentResult(resultId, order.orderReference(), order.customerId(),
                 order.totalAmount(),status, charge.reason());
        outbox.save(OutboxEvent.pending(resultId, Constants.PAYMENT_UPPER, order.orderReference(),
                Constants.PAYMENT_TOPIC, json.writeValueAsString(result)));
        outbox.save(OutboxEvent.pendingRabbit(emailId, Constants.PAYMENT_UPPER, order.orderReference(),
                RabbitTopology.EMAIL_EXCHANGE, RabbitTopology.KEY_PAYMENT, json.writeValueAsString(email)));
    }

    @Transactional(readOnly = true)
    public PaymentView findByOrderReference(String reference) {
        var payment = payments.findByOrderReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Constants.PAYMENT,
                        reference
                ));

        return mapper.toPaymentView(payment);
    }
}