package com.payshield.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recon_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconRecord {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "txn_amount", precision = 18, scale = 2)
    private BigDecimal txnAmount;

    @Column(name = "settlement_amount", precision = 18, scale = 2)
    private BigDecimal settlementAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconStatus status = ReconStatus.PENDING;

    @Column(name = "mismatch_reason")
    private String mismatchReason;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ReconStatus { PENDING, MATCHED, MISMATCH, MISSING, EXTRA }
}
