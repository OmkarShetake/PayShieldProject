package com.payshield.reconciliation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Settlement {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_ref", nullable = false, unique = true)
    private String bankRef;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "settled_at", nullable = false)
    private LocalDateTime settledAt;

    @Column(name = "bank_name")
    private String bankName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
