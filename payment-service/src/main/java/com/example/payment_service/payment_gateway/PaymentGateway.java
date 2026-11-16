package com.example.payment_service.payment_gateway;



import java.math.BigDecimal;

public interface PaymentGateway {

    /** @param idempotencyKey a real gateway (Stripe...) uses it to never charge the same key twice */
    ChargeResult charge(String idempotencyKey, BigDecimal amount);

    record ChargeResult(boolean success, String reason) {}
}

