package com.example.product_service.service;

import com.example.product_service.repository.ProductRepository;
import com.example.product_service.repository.StockMovementRepository;
import com.example.product_service.repository.StockRestorationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRestorationService {

    private final StockRestorationRepository restorations;
    private final StockMovementRepository movements;
    private final ProductRepository products;

    @Transactional
    public void restore(UUID purchaseId) {
        // CLAIM first, in the same transaction as the restore: a duplicate PaymentFailed gets false and stops here;
        // if anything below fails, the claim rolls back and the retry can restore.
        if (!restorations.claim(purchaseId)) {
            log.info("Stock of purchase {} was already restored, ignoring", purchaseId);
            return;
        }
        var rows = movements.findByPurchaseId(purchaseId);
        if (rows.isEmpty()) {
            log.warn("Purchase {} has no stock movements: nothing to restore", purchaseId);
            return;
        }
        rows.forEach(m -> products.increaseStock(m.getProductId(), m.getQuantity()));
        log.info("Restored the stock of purchase {} ({} products)", purchaseId, rows.size());
    }
}