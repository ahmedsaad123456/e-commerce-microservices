package com.example.order_service.customer;

import com.example.order_service.common.RemoteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


// Declares an HTTP client for communicating with the Customer service.
// Feign generates the HTTP client implementation automatically at runtime.
@FeignClient(
        name = "customer-service",
        contextId = "customerFeign",
        path = "/api/v1/customers",
        configuration = CustomerFeignSecurityConfig.class

)
public interface CustomerFeignClient {

    // Calls GET /api/v1/customers/exists/{customer-id}.
    // The response is deserialized into RemoteResponse<Boolean>.
    @GetMapping("/exists/{customer-id}")
    RemoteResponse<Boolean> exists(
            @PathVariable("customer-id") String customerId
    );


}
