package com.example.order_service.exceptions;

// the ONLY extra exception: Resilience4j recognises "retryable / counts as a failure" by this class
public class DependencyUnavailableException extends RuntimeException {   // retryable
    public DependencyUnavailableException(String service, String reason, Throwable cause) {
        super(service + " is unavailable: " + reason, cause);
    }
}
