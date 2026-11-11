package com.example.product_service.repository;

import com.example.product_service.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    boolean existsByPurchaseId(UUID purchaseId);

    List<StockMovement> findAllByPurchaseId(UUID purchaseId);
}
