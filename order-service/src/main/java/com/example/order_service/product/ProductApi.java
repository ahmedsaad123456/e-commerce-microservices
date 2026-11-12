package com.example.order_service.product;

import com.example.order_service.dto.PurchaseRequest;
import com.example.order_service.dto.PurchaseResponse;

import java.util.List;

// what the Order service needs from the other services (independent of Feign / RestClient)
public interface ProductApi  { List<PurchaseResponse> purchase(PurchaseRequest request); }

