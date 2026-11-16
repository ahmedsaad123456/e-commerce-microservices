package com.example.payment_service.mapper;

import com.example.payment_service.dto.PaymentView;
import com.example.payment_service.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentView toPaymentView(Payment payment);
}