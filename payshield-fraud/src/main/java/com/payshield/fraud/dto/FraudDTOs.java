package com.payshield.fraud.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FraudDTOs {

    @Data
    public static class FraudCheckRequest {
        private UUID transactionId;
        private UUID merchantId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String customerEmail;
        private String customerPhone;
    }

    @Data
    @Builder
    public static class FraudCheckResult {
        private UUID transactionId;
        private BigDecimal score;
        private boolean flagged;
        private String decision;        // APPROVE | FLAG | REJECT
        private List<String> triggeredRules;
    }

    @Data
    public static class AiScorerRequest {
        private String transactionId;
        private double amount;
        private String paymentMethod;
        private int hour;
        private Map<String, Object> features;
    }

    @Data
    public static class AiScorerResponse {
        private double score;
        private String modelVersion;
        private Map<String, Double> featureImportance;
    }

    @Data
    public static class FraudAlertResponse {
        private UUID transactionId;
        private UUID merchantId;
        private String alertType;
        private String severity;
        private String description;
        private boolean resolved;
        private String createdAt;
    }
}
