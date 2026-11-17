package com.example.order_service.customer;

import com.example.order_service.common.RemoteErrors;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;



// Apply Adapter patterns
@Component
// Activates this implementation when clients.type=feign.
// Uses Feign as the HTTP client when the property is missing.
@ConditionalOnProperty(
        name = "clients.type",
        havingValue = "feign",
        matchIfMissing = true
)
@RequiredArgsConstructor
class FeignCustomerApi implements CustomerApi {

    // Feign client responsible for sending HTTP requests to Customer service.
    private final CustomerFeignClient feign;

    @Override
    public boolean exists(String customerId) {

        try {
            // Extracts the data field from the remote API response.
            return Boolean.TRUE.equals(feign.exists(customerId).data());

        } catch (FeignException e) {

            // Converts Feign-specific HTTP errors into application exceptions.
            throw RemoteErrors.translate(
                    "customer-service",
                    e.status(),
                    e.contentUTF8(),
                    e
            );
        }
    }


}