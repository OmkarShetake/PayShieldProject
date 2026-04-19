package com.payshield.payment.dto;

import com.payshield.payment.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class PaymentDTOs {

    @Data
    public static class InitiatePaymentRequest {
        @NotNull @DecimalMin("1.00")
        private BigDecimal amount;

        @NotBlank @Size(min = 3, max = 3)
        private String currency = "INR";

        @NotNull
        private Transaction.PaymentMethod paymentMethod;

        @Email
        private String customerEmail;

        private String customerPhone;
        private String description;
        private String externalRef;
        private Map<String, Object> metadata;
    }

    @Data
    public static class PaymentResponse {
        private UUID id;
        private String externalRef;
        private BigDecimal amount;
        private String currency;
        private Transaction.TransactionStatus status;
        private Transaction.PaymentMethod paymentMethod;
        private String customerEmail;
        private BigDecimal fraudScore;
        private boolean fraudFlagged;
        private LocalDateTime initiatedAt;
        private LocalDateTime completedAt;

        public static PaymentResponse from(Transaction t) {
            PaymentResponse r = new PaymentResponse();
            r.id = t.getId();
            r.externalRef = t.getExternalRef();
            r.amount = t.getAmount();
            r.currency = t.getCurrency();
            r.status = t.getStatus();
            r.paymentMethod = t.getPaymentMethod();
            r.customerEmail = t.getCustomerEmail();
            r.fraudScore = t.getFraudScore();
            r.fraudFlagged = t.isFraudFlagged();
            r.initiatedAt = t.getInitiatedAt();
            r.completedAt = t.getCompletedAt();
            return r;
        }
    }

    @Data
    public static class PaymentListResponse {
        private java.util.List<PaymentResponse> transactions;
        private long total;
        private int page;
        private int size;
    }

    @Data
    public static class RefundRequest {
        @NotNull @DecimalMin("1.00")
        private BigDecimal amount;

        @NotBlank
        private String reason;
    }

    // Kafka event payload
    @Data
    public static class PaymentEvent {
        private UUID transactionId;
        private UUID merchantId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String paymentMethod;
        private String customerEmail;
        private String customerPhone;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
        private boolean fraudFlagged;
    }
}
