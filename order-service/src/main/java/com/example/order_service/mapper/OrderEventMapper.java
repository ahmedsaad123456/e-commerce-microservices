package com.example.order_service.mapper;

import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderConfirmation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderEventMapper {

    @Mapping(target = "eventId",        source = "eventId")                // the method parameter
    @Mapping(target = "orderReference", source = "order.reference")        // UUID -> String (built in)
    @Mapping(target = "customerId",     source = "order.customerId")
    @Mapping(target = "totalAmount",    source = "order.totalAmount")
    @Mapping(target = "paymentMethod",  source = "order.paymentMethod")
    @Mapping(target = "lines",          source = "order.lines")            // line entity -> Line is generated automatically
    OrderConfirmation toOrderConfirmation(Order order, UUID eventId);
}