package com.example.notification_service.common;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class ServiceTokenProvider {

    private final RestClient restClient;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(@Value("${keycloak.token-url}") String tokenUrl,
                                @Value("${keycloak.client-id}") String clientId,
                                @Value("${keycloak.client-secret}") String clientSecret) {
        // IMPORTANT: a plain client on purpose. Do NOT inject your @LoadBalanced RestClient.Builder here:
        // Keycloak is not registered in Eureka, so the load balancer would fail to find a service called "keycloak".
        this.restClient = RestClient.create();
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // synchronized: two worker threads must not both ask Keycloak for a token at the same moment
    public synchronized String getToken() {
        // Renew 30 seconds BEFORE expiry so a token never expires while a request is in flight
        if (cachedToken == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);

            TokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            cachedToken = response.accessToken();
            expiresAt = Instant.now().plusSeconds(response.expiresIn());
        }
        return cachedToken;
    }

    // Maps the JSON fields of Keycloak's answer: { "access_token": "...", "expires_in": 300, ... }
    // (unknown fields are ignored by default in Spring Boot's JSON configuration)
    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {}
}