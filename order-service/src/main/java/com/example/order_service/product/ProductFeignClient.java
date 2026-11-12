package com.example.order_service.product;

import com.example.order_service.common.RemoteResponse;
import com.example.order_service.dto.PurchaseRequest;
import com.example.order_service.dto.PurchaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service", contextId = "productFeign", path = "/api/v1/products")
public interface ProductFeignClient {
    @PostMapping("/purchase")
    RemoteResponse<List<PurchaseResponse>> purchase(@RequestBody PurchaseRequest request);
}

