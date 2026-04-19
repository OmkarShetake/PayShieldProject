package com.payshield.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ReportDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardResponse {
        private UUID merchantId;
        private int periodDays;
        private long totalTransactions;
        private long successfulTransactions;
        private long failedTransactions;
        private long fraudFlagged;
        private BigDecimal totalVolume;
        private BigDecimal avgTransactionValue;
        private BigDecimal successRate;
        private List<DailyVolume> dailyVolumes;
    }

    @Data @AllArgsConstructor
    public static class DailyVolume {
        private String date;
        private BigDecimal volume;
        private long transactionCount;
    }

    // Interface-based projection for JPQL query result mapping
    public interface PaymentMethodStat {
        String getPaymentMethod();
        Long getCount();
        BigDecimal getVolume();
    }
}
