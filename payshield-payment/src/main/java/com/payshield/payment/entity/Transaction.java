package com.payshield.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "external_ref", unique = true)
    private String externalRef;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    private String description;

    @Column(name = "fraud_score", precision = 5, scale = 2)
    private BigDecimal fraudScore;

    @Column(name = "fraud_flagged")
    private boolean fraudFlagged = false;

    @Column(name = "gateway_txn_id")
    private String gatewayTxnId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "initiated_at", updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        initiatedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TransactionStatus {
        INITIATED, PENDING, FRAUD_CHECK, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }

    public enum PaymentMethod {
        CARD, UPI, NET_BANKING, WALLET, BANK_TRANSFER
    }
}
