package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderReference(String orderReference);

    // "Claim" the order. Returns 1 only for the FIRST event of this order; a duplicate gets 0.
    // We do NOT check "does a payment exist?" and then insert: two events at the same moment would both
    // pass the check. A single INSERT + the unique constraint lets the database decide, atomically.
    @Modifying
    @Query(value = """
            INSERT INTO payments (id, order_reference, customer_id, amount, status)
            VALUES (:id, :orderReference, :customerId, :amount, 'PROCESSING')
            ON CONFLICT (order_reference) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("id") UUID id, @Param("orderReference") String orderReference,
              @Param("customerId") String customerId, @Param("amount") BigDecimal amount);
}