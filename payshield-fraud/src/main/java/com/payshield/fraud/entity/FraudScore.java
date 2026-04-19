package com.payshield.fraud.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fraud_scores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FraudScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private boolean flagged;

    @Column(name = "model_version", nullable = false)
    private String modelVersion = "v1.0";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_triggers", columnDefinition = "jsonb")
    private List<String> ruleTriggers;

    @Column(nullable = false)
    private String decision; // APPROVE, FLAG, REJECT

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
