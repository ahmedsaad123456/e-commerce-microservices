package com.example.order_service.customer;
import com.example.order_service.common.ErrorCode;
import com.example.order_service.exceptions.AppException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomerGateway {

    // Depends on the abstraction instead of a specific HTTP client.
    private final CustomerApi api;

    // Retries failed calls according to the configured customer retry policy.
    // Opens the circuit when the configured failure threshold is reached.
    // Calls existsFallback when the circuit breaker rejects the request.
    @Retry(name = "customer")
    @CircuitBreaker(name = "customer", fallbackMethod = "existsFallback")
    public boolean exists(String customerId) {

        // Delegates the actual communication to the selected CustomerApi implementation.
        return api.exists(customerId);
    }

    // Handles requests rejected because the circuit breaker is OPEN.
    // Throws a service-unavailable exception instead of calling the remote service.
    private boolean existsFallback(
            CallNotPermittedException e
    ) {
        throw new AppException(
                ErrorCode.SERVICE_UNAVAILABLE,
                e,
                "customer-service",
                "circuit is open"
        );
    }
}