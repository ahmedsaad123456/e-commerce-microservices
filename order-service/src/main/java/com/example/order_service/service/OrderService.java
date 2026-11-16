package com.example.order_service.service;

import com.example.order_service.common.Constants;
import com.example.order_service.common.ErrorCode;
import com.example.order_service.customer.CustomerGateway;
import com.example.order_service.dto.*;
import com.example.order_service.entity.Order;
import com.example.order_service.exceptions.AppException;
import com.example.order_service.exceptions.ResourceNotFoundException;
import com.example.order_service.mapper.OrderMapper;
import com.example.order_service.outbox.OrderFailureWriter;
import com.example.order_service.outbox.OrderWriter;
import com.example.order_service.product.ProductGateway;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CustomerGateway customers;
    private final ProductGateway products;
    private final OrderRepository orders;
    private final OrderMapper orderMapper;
    private final OrderFailureWriter orderFailureWriter;
    private final OrderWriter orderWriter;
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

        // If the order service is killed here, we need some solutions to restore the stock
        // we can make the product receives the order confirmation to ensure the deduction
        // at the same time we need schedule to look at the db, if any product stock doesn't have the confirmation
        // we will restore the products of that transaction to the stock


        try {
            // 3) Build the order using the Product service response.
            var order = orderMapper.toOrder(request, purchased, reference);
            orderMapper.addOrderLines(order, purchased);

            // 4) Save and return the response.
            var savedOrder = orderWriter.saveWithEvent(order);
            return orderMapper.toOrderResponse(savedOrder);

        } catch (RuntimeException e) {
            publishFailure(reference, e);      // tell the Product service to give the stock back
            throw e;                           // the client still receives the original error
        }
    }


    private void publishFailure(UUID reference, RuntimeException cause) {
        // The try/catch is HERE and not inside the writer: if the writer's own transaction fails, catching the error
        // inside it would still make Spring throw UnexpectedRollbackException at commit
        try {
            orderFailureWriter.recordFailure(reference, cause);
        } catch (RuntimeException ex) {
            log.error("Could not record the failure of purchase {}: the Product reconciliation job will release it",
                    reference, ex);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse findOrderById(Long id) {

        var order = orders.findWithLinesById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.ORDER, id));

        return orderMapper.toOrderResponse(order);
    }



    @Transactional(readOnly = true)
    public List<OrderLineResponse> findLines(Long orderId) {
        return loadWithLines(orderId).getLines().stream()
                .map(orderMapper::toOrderLineResponse)
                .toList();
    }

    private Order loadWithLines(Long id) {
        // 404 when the order does not exist (an empty list would hide a wrong id from the caller)
        return orders.findWithLinesById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, Constants.ORDER, id));
    }

}