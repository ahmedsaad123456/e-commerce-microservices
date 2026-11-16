package com.example.payment_service.controller;

import com.example.payment_service.common.ApiResponse;
import com.example.payment_service.dto.PaymentView;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order/{order-reference}")
    public ApiResponse<PaymentView> getPaymentByOrder(
            @PathVariable("order-reference") String reference) {

        return ApiResponse.ok(paymentService.findByOrderReference(reference));
    }

}