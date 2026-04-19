package com.payshield.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recon_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconRun {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private String status = "RUNNING";

    @Column(name = "from_date", nullable = false)
    private LocalDateTime fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDateTime toDate;

    @Column(name = "total_txns")
    private int totalTxns;

    private int matched;
    private int mismatched;
    private int missing;

    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @PrePersist protected void onCreate() { startedAt = LocalDateTime.now(); }
}
