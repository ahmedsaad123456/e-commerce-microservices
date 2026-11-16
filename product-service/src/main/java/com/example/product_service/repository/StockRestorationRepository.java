package com.example.product_service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockRestorationRepository {

    private final JdbcTemplate jdbc;

    // Marks the purchase as "restored". Returns true only for the FIRST caller;
    // a duplicate gets false because of ON CONFLICT DO NOTHING (0 rows inserted).
    // MANDATORY: must run inside the caller's transaction, so the claim is rolled back
    // together with the restore if anything fails.
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(UUID purchaseId) {
        return jdbc.update(
                "INSERT INTO stock_restorations (purchase_id) VALUES (?) ON CONFLICT DO NOTHING",
                purchaseId) == 1;
    }
}