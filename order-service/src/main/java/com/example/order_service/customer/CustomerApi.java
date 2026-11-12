package com.example.order_service.customer;

// Defines the operations that the Order service needs from the Customer service.
// This abstraction keeps the business logic independent of the HTTP client implementation.  (independent of Feign / RestClient)
public interface CustomerApi {

    // Checks whether a customer exists in the Customer service.
    boolean exists(String customerId);
}