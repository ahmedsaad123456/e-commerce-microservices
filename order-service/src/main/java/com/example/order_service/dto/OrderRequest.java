package com.example.order_service.dto;

import com.example.order_service.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotBlank String customerId,
        @NotNull PaymentMethod paymentMethod,
        @NotEmpty List<@Valid OrderLineRequest> lines) {}

