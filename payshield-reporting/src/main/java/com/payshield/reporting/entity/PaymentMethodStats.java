package com.payshield.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_method_stats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentMethodStats {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Builder.Default
    private long count = 0;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal volume = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
