package com.example.order_service.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // generic
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    // answers that come from other services
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    REMOTE_NOT_FOUND(HttpStatus.NOT_FOUND),
    REMOTE_REJECTED(HttpStatus.BAD_GATEWAY);


    private final HttpStatus status;

    public String messageKey() {            // the key used inside messages.properties
        return "error." + name();
    }
}