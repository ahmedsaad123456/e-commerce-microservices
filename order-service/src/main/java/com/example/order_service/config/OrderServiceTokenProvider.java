package com.example.order_service.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class OrderServiceTokenProvider {

    private final RestClient restClient;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public OrderServiceTokenProvider(
            @Value("${keycloak.token-url}") String tokenUrl,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret
    ) {
        // Keycloak is accessed directly, not through Eureka.
        this.restClient = RestClient.create();

        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getToken() {

        // Refresh the token before it expires.
        if (cachedToken == null ||
                Instant.now().isAfter(expiresAt.minusSeconds(30))) {

            MultiValueMap<String, String> form =
                    new LinkedMultiValueMap<>();

            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);

            TokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null ||
                    response.accessToken() == null ||
                    response.accessToken().isBlank()) {
                throw new IllegalStateException(
                        "Failed to obtain Order Service access token"
                );
            }

            cachedToken = response.accessToken();

            expiresAt = Instant.now()
                    .plusSeconds(response.expiresIn());
        }

        return cachedToken;
    }

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {}
}