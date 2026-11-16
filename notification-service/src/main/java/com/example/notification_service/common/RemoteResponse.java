package com.example.notification_service.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteResponse<T>(T data, String message) {}      // the part of the {data, message, ...} envelope we read

