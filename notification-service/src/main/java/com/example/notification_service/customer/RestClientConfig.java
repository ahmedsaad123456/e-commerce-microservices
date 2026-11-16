package com.example.notification_service.customer;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;


@Configuration
public class RestClientConfig {


    // Plain builder: Eureka and anything else injecting RestClient.Builder gets this one.
    @Bean
    @Primary
    RestClient.Builder plainRestClientBuilder() {
        return RestClient.builder();
    }

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
        // Creates a RestClient.Builder that supports service discovery and client-side load balancing.
    RestClient.Builder loadBalancedRestClientBuilder() {

        // Creates the underlying HTTP client with a connection timeout of 2 seconds.
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );

        // Limits the time allowed to wait for the server's response to 3 seconds.
        factory.setReadTimeout(Duration.ofSeconds(3));

        // Returns a RestClient.Builder using the configured HTTP request factory.
        return RestClient.builder().requestFactory(factory);
    }
}

