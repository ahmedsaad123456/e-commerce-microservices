package com.example.order_service.mapper;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderLine;
import org.mapstruct.Mapper;


import com.example.order_service.dto.*;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reference", source = "reference")
    @Mapping(target = "customerId", source = "request.customerId")
    @Mapping(target = "paymentMethod", source = "request.paymentMethod")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "totalAmount", expression = "java(calculateTotal(purchased))")
    @Mapping(target = "lines", ignore = true)
    Order toOrder(
            OrderRequest request,
            List<PurchaseResponse> purchased,
            UUID reference
    );

    @Mapping(target = "productName" , source = "name")
    OrderLine toOrderLine(PurchaseResponse purchased);

    OrderResponse toOrderResponse(Order order);

    default BigDecimal calculateTotal(List<PurchaseResponse> purchased) {
        return purchased.stream()
                .map(p -> p.unitPrice().multiply(BigDecimal.valueOf(p.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default void addOrderLines(Order order, List<PurchaseResponse> purchased) {
        purchased.stream()
                .map(this::toOrderLine)
                .forEach(order::addLine);
    }
}