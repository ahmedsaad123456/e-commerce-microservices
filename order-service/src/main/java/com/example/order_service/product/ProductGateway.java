package com.example.order_service.product;

import com.example.order_service.common.ErrorCode;
import com.example.order_service.dto.PurchaseRequest;
import com.example.order_service.dto.PurchaseResponse;
import com.example.order_service.exceptions.AppException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductGateway {

    private final ProductApi api;

    // Retry is the outer layer, the circuit breaker is the inner one: every attempt goes through the breaker
    @Retry(name = "product")
    @CircuitBreaker(name = "product", fallbackMethod = "purchaseFallback")
    @Bulkhead(name = "product")
    public List<PurchaseResponse> purchase(PurchaseRequest request) {
        return api.purchase(request);
    }

    // used only when the circuit is OPEN: fail fast with a clear exception (never retried)
    private List<PurchaseResponse> purchaseFallback(CallNotPermittedException e) {
        throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, e, "product-service", "circuit is open");
    }

    private List<PurchaseResponse> purchaseFallback(BulkheadFullException e) {
        throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, e, "product-service", "bulkhead is full");
    }


}

