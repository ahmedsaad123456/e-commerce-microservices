package com.example.payment_service.payment_gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private final BigDecimal declineAmount;

    public FakePaymentGateway(@Value("${payment.fake-gateway.decline-amount}") BigDecimal declineAmount) {
        this.declineAmount = declineAmount;
    }

    @Override
    public ChargeResult charge(String idempotencyKey, BigDecimal amount) {
        if (amount.compareTo(declineAmount) == 0) {
            return new ChargeResult(false, "Card declined");
        }
        return new ChargeResult(true, null);
    }
}
