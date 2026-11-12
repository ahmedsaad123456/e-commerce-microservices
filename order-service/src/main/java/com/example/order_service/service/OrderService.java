package com.example.order_service.service;

import com.example.order_service.common.Constants;
import com.example.order_service.customer.CustomerGateway;
import com.example.order_service.dto.*;
import com.example.order_service.exceptions.ResourceNotFoundException;
import com.example.order_service.mapper.OrderMapper;
import com.example.order_service.product.ProductGateway;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerGateway customers;
    private final ProductGateway products;
    private final OrderRepository orders;
    private final OrderMapper mapper;

    // Never keep a database transaction open while waiting for another service over HTTP.
    public OrderResponse createOrder(OrderRequest request) {

        // 1) Verify customer existence.
        if (!customers.exists(request.customerId())) {
            throw new ResourceNotFoundException(Constants.CUSTOMER, request.customerId());
        }

        // 2) Create the reference once per attempt; it acts as the idempotency key.
        var reference = UUID.randomUUID();

        var items = request.lines().stream()
                .map(l -> new PurchaseItem(l.productId(), l.quantity()))
                .toList();

        var purchased = products.purchase(new PurchaseRequest(reference, items));



        // 3) Build the order using the Product service response.
        var order = mapper.toOrder(request, purchased, reference);

        mapper.addOrderLines(order, purchased);

        // 4) Save and return the response.
        var savedOrder = orders.save(order);

        return mapper.toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse findOrderById(Long id) {

        var order = orders.findWithLinesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.ORDER, id));

        return mapper.toOrderResponse(order);
    }
}