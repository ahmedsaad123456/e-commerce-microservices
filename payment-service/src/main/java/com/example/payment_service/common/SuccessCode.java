package com.example.payment_service.common;

public enum SuccessCode {
    USER_REGISTERED,
    USER_AUTHENTICATED,

    USER_REFRESH_TOKEN;

    public String messageKey() {
        return "success." + name();
    }
}