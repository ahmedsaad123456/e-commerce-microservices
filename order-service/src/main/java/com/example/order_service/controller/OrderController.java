package com.example.order_service.controller;


import com.example.order_service.common.ApiResponse;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @RequestBody @Valid OrderRequest request
    ) {
        return ApiResponse.ok("Order created successfully", service.createOrder(request));
    }

    @GetMapping("/{order-id}")
    public ApiResponse<OrderResponse> findOrderById(
            @PathVariable("order-id") Long id
    ) {
        return ApiResponse.ok("Order retrieved successfully", service.findOrderById(id));
    }
}
