
package com.example.notification_service.customer;

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.MDC;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    // Plain builder for direct HTTP calls that do not require service discovery.
    @Bean
    @Primary
    RestClient.Builder plainRestClientBuilder(
            ObservationRegistry observationRegistry,
            ClientHttpRequestInterceptor correlationIdInterceptor) {

        return RestClient.builder()
                .observationRegistry(observationRegistry)
                .requestInterceptor(correlationIdInterceptor);
    }

    // Load-balanced builder for calls to services registered in Eureka.
    // For example, "http://customer-service" uses the Eureka service ID.
    @Bean
    // @LoadBalanced makes this builder "Eureka-aware".
    // "http://customer-service" is NOT a real host name: it is the Eureka service id (spring.application.name).
    // Before every request, Spring Cloud LoadBalancer does three things:
    //   1) asks Eureka for all running instances of "customer-service"
    //   2) picks one of them (round-robin by default)
    //   3) replaces the service id in the URL with that instance's real host:port
    // Benefits: no hard-coded addresses or ports, calls are spread across instances, dead instances are skipped.
    // Warning: EVERY RestClient built from this builder resolves host names through Eureka,
    // so never use it to call external URLs (use RestClient.create() for those).
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            ObservationRegistry observationRegistry,
            ClientHttpRequestInterceptor correlationIdInterceptor) {

        // Configure the underlying HTTP client with a 2-second connection timeout.
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );

        // Allow up to 3 seconds to wait for the server response.
        factory.setReadTimeout(Duration.ofSeconds(3));

        // Enable tracing observations and propagate the correlation ID.
        return RestClient.builder()
                .requestFactory(factory)
                .observationRegistry(observationRegistry)
                .requestInterceptor(correlationIdInterceptor);
    }

    // Propagate the correlation ID to downstream services.
    @Bean
    ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (request, body, execution) -> {
            String id = MDC.get("correlationId");

            if (id != null) {
                request.getHeaders().set("X-Correlation-Id", id);
            }

            return execution.execute(request, body);
        };
    }
}
