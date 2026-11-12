package com.example.order_service.product;

import com.example.order_service.dto.PurchaseRequest;
import com.example.order_service.dto.PurchaseResponse;
import com.example.order_service.common.RemoteErrors;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "clients.type", havingValue = "feign", matchIfMissing = true)
@RequiredArgsConstructor
class FeignProductApi implements ProductApi {
    private final ProductFeignClient feign;

    @Override
    public List<PurchaseResponse> purchase(PurchaseRequest request) {
        try {
            return feign.purchase(request).data();
        } catch (FeignException e) {
            // status -1 means an I/O problem (refused, timeout)
            throw RemoteErrors.translate("product-service", e.status(), e.contentUTF8(), e);
        }
    }
}
