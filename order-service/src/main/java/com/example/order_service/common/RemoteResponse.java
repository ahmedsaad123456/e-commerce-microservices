package com.example.order_service.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// the part of the other services' envelope that the Order service reads
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteResponse<T>(T data, String message) {}