
package com.example.customer.controller;

import com.example.customer.common.ApiResponse;
import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ApiResponse<CustomerResponse> createCustomer(
            @RequestBody @Valid CustomerRequest request
    ) {
        CustomerResponse result = this.service.createCustomer(request);
        return ApiResponse.ok("Customer created successfully", result);
    }

    @PutMapping("/{customer-id}")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable("customer-id") String customerId,
            @RequestBody @Valid CustomerRequest request
    ) {
        CustomerResponse response = this.service.updateCustomer(customerId, request);
        return ApiResponse.ok("Customer updated successfully", response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CustomerResponse>> findAll() {
        List<CustomerResponse> customers = this.service.findAllCustomers();
        return ApiResponse.ok("Customers retrieved successfully", customers);
    }

    @GetMapping("/exists/{customer-id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SERVICE') or #customerId == authentication.name"
    )
    public ApiResponse<Boolean> existsById(
            @PathVariable("customer-id") String customerId
    ) {
        boolean exists = this.service.existsById(customerId);
        return ApiResponse.ok("Customer existence checked", exists);
    }

    @GetMapping("/{customer-id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SERVICE') or #customerId == authentication.name"
    )
    public ApiResponse<CustomerResponse> findById(
            @PathVariable("customer-id") String customerId
    ) {
        CustomerResponse customer = this.service.findById(customerId);
        return ApiResponse.ok("Customer retrieved successfully", customer);
    }

    @DeleteMapping("/{customer-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("customer-id") String customerId
    ) {
        this.service.deleteCustomer(customerId);
    }

}
