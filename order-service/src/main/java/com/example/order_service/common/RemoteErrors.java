package com.example.order_service.common;

import com.example.order_service.exceptions.AppException;
import com.example.order_service.exceptions.DependencyUnavailableException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class RemoteErrors {
    private RemoteErrors() {
        /* This utility class should not be instantiated */
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();   // Jackson 3

    public static RuntimeException translate(String service, int status, String body, Throwable cause) {
        var message = messageOf(body);
        return switch (status) {
            case 404 -> new AppException(ErrorCode.REMOTE_NOT_FOUND, cause, message);
            case 409 -> new AppException(ErrorCode.INSUFFICIENT_STOCK, cause, message);
            default -> (status >= 400 && status < 500)
                    ? new AppException(ErrorCode.REMOTE_REJECTED, cause, service, message)
                    : new DependencyUnavailableException(service, "HTTP " + status, cause);   // 5xx or -1 (I/O)
        };
    }

    private static String messageOf(String body) {
        try {
            JsonNode messageNode = JSON.readTree(body).path("message");

            return messageNode.isString()
                    ? messageNode.asString()
                    : "(no message)";
        } catch (RuntimeException e) {
            return "(unreadable response)";
        }
    }
}