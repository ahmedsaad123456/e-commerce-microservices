package com.example.notification_service.customer;

import com.example.notification_service.common.RemoteResponse;
import com.example.notification_service.common.ServiceTokenProvider;
import com.example.notification_service.entity.CustomerView;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;


@Component
public class CustomerClient {

    private final RestClient client;

    private final ServiceTokenProvider tokens;

    // Injects the load-balanced builder and configures the Customer Service as the base URL.
    public CustomerClient(@LoadBalanced RestClient.Builder loadBalanced,  ServiceTokenProvider tokens) {
        this.tokens = tokens;

        // Uses the logical service name instead of a fixed IP address or hostname.
        // Eureka resolves "customer-service" to a registered instance,
        // and the load balancer selects an available instance for each request.
        this.client = loadBalanced
                .baseUrl("http://customer-service")
                // Runs before EVERY call: attaches "Authorization: Bearer <service token>"
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(this.tokens.getToken());
                    return execution.execute(request, body);
                })
                .build();
    }

    public CustomerView findCustomerById(String customerId) {
        try {

            // Sends a GET request to the selected Customer Service instance.
            // The load balancer resolves the service name and selects an instance.
            // ParameterizedTypeReference preserves the generic RemoteResponse<CustomerView> type.
            var response = client.get()
                    .uri("/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<RemoteResponse<CustomerView>>() {
                    });

            if (response == null || response.data() == null) {
                throw new IllegalStateException(
                        "Customer service returned an empty response for customer " + customerId);
            }
            return response.data();

        } catch (HttpClientErrorException.NotFound e) {

            // A missing customer is a business error, not a temporary network failure.
            // Retrying will not make a non-existent customer appear.
            // Convert the 404 response into an IllegalArgumentException,
            // which KafkaErrorConfig treats as non-retryable.
            throw new IllegalArgumentException(
                    "Customer " + customerId + " does not exist", e);
        }
    }
}

