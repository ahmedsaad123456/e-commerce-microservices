package com.example.order_service.controller;


import com.example.order_service.common.ApiResponse;
import com.example.order_service.config.CurrentUser;
import com.example.order_service.dto.OrderLineResponse;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @RequestBody @Valid OrderRequest request,
            Authentication auth
    ) {
        return ApiResponse.ok("Order created successfully", service.createOrder(request, CurrentUser.from(auth)));
    }

    // "me" means "the owner of the token". Spring prefers this literal path over "/{order-id}".
    @GetMapping("/me")
    public ApiResponse<List<OrderResponse>> findMyOrders(Authentication auth) {
        return ApiResponse.ok("Orders retrieved successfully", service.findMyOrders(CurrentUser.from(auth)));
    }

    @GetMapping("/{order-id}")
    public ApiResponse<OrderResponse> findOrderById(
            @PathVariable("order-id") Long id,
            Authentication auth
    ) {
        return ApiResponse.ok("Order retrieved successfully", service.findOrderById(id, CurrentUser.from(auth)));
    }

    @GetMapping("/order-line/{order-id}")
    public ApiResponse<List<OrderLineResponse>> findListOrderLinesByOrder(
            @PathVariable("order-id") Long orderId,
            Authentication auth
    ) {
        return ApiResponse.ok(service.findLines(orderId, CurrentUser.from(auth)));
    }
}