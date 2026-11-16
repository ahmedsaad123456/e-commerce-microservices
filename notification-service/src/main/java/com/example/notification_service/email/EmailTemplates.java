package com.example.notification_service.email;

import com.example.notification_service.entity.CustomerView;
import com.example.notification_service.entity.Notification;

public final class EmailTemplates {

    private EmailTemplates() {}

    public static String subject(Notification n) {
        return switch (n.getType()) {
            case ORDER_RECEIVED -> "We received your order " + n.getOrderReference();
            case PAYMENT_SUCCEEDED -> "Payment confirmed for order " + n.getOrderReference();
            case PAYMENT_FAILED -> "Payment failed for order " + n.getOrderReference();
        };
    }

    public static String body(Notification n, CustomerView customer) {
        var intro = "Hello " + customer.firstname() + ",\n\n";
        return intro + switch (n.getType()) {
            case ORDER_RECEIVED -> "Thank you! Your order total is " + n.getAmount() + ". We are processing the payment.";
            case PAYMENT_SUCCEEDED -> "We received your payment of " + n.getAmount() + ". Your order is confirmed.";
            case PAYMENT_FAILED -> "We could not charge " + n.getAmount() + " (" + n.getDetail() + "). Your order was cancelled.";
        };
    }
}