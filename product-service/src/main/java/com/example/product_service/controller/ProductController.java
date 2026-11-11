package com.example.product_service.controller;

import com.example.product_service.common.ApiResponse;
import com.example.product_service.dto.*;
import com.example.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductRequest request) {
        return ApiResponse.ok("Product created successfully", service.createProduct(request));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> findProductAll() {
        return ApiResponse.ok("Products retrieved successfully", service.findProductAll());
    }

    @GetMapping("/{product-id}")
    public ApiResponse<ProductResponse> findProductById(@PathVariable("product-id") Integer id) {
        return ApiResponse.ok("Product retrieved successfully", service.findProductById(id));
    }

    @PutMapping("/{product-id}")
    public ApiResponse<ProductResponse> updateCustomer(
            @PathVariable("product-id") Integer productId,
            @RequestBody @Valid ProductRequest request
    ) {
        ProductResponse response = this.service.updateProduct(productId, request);
        return ApiResponse.ok("Product updated successfully", response);
    }

    @GetMapping("/{product-id}/availability")
    public ApiResponse<StockResponse> checkProductAvailability(@PathVariable("product-id") Integer id) {
        return ApiResponse.ok("Availability retrieved successfully", service.checkProductAvailability(id));
    }

    @PostMapping("/purchase")
    public ApiResponse<List<PurchaseResponse>> purchase(@RequestBody @Valid PurchaseRequest request) {
        return ApiResponse.ok("Products purchased successfully", service.purchase(request));
    }
}