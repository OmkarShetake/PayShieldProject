package com.payshield.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_summary")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionSummary {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "total_transactions")
    private long totalTransactions;

    @Builder.Default
    private long successful = 0;

    @Builder.Default
    private long failed = 0;

    @Column(name = "flagged_fraud")
    @Builder.Default
    private long flaggedFraud = 0;

    @Column(name = "total_volume", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalVolume = BigDecimal.ZERO;

    @Column(name = "avg_transaction", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal avgTransaction = BigDecimal.ZERO;

    @Column(name = "success_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal successRate = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
