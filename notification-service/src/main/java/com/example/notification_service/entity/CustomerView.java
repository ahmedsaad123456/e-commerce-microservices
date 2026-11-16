package com.example.notification_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerView(String id, String firstname, String lastname, String email) {
}
