package com.example.gateway.controller;


import com.example.gateway.config.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;



@RestController
@RequestMapping("/fallback")
public class FallbackController {

    // Keep the original HTTP method (GET, POST, etc.) when handling the fallback.
    @RequestMapping("/orders")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ApiResponse<Void>> orders() {

        // Return a clear error response: the order was NOT placed.
        return Mono.just(ApiResponse.error(
                "ORDER_SERVICE_UNAVAILABLE",
                "Order service is temporarily unavailable. Please try again shortly."
        ));
    }
}
