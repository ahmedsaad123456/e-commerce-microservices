package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    private UUID id;
    @Column(nullable = false, length = 64) private String orderReference;
    @Column(nullable = false, length = 64) private String customerId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 200) private String reason;
    @Column(insertable = false, updatable = false) private Instant createdAt;

    public void finish(PaymentStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}