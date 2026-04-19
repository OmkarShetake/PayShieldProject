package com.payshield.reconciliation.dto;

import com.payshield.reconciliation.entity.ReconRun;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReconDTOs {

    @Data
    public static class ReconRequest {
        @NotNull private UUID merchantId;
        @NotNull private LocalDateTime fromDate;
        @NotNull private LocalDateTime toDate;
        private String triggeredBy = "SYSTEM";
    }

    @Data
    public static class ReconRunResponse {
        private UUID id;
        private UUID merchantId;
        private String status;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;
        private int totalTxns;
        private int matched;
        private int mismatched;
        private int missing;
        private double matchRate;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        public static ReconRunResponse from(ReconRun run) {
            ReconRunResponse r = new ReconRunResponse();
            r.id = run.getId();
            r.merchantId = run.getMerchantId();
            r.status = run.getStatus();
            r.fromDate = run.getFromDate();
            r.toDate = run.getToDate();
            r.totalTxns = run.getTotalTxns();
            r.matched = run.getMatched();
            r.mismatched = run.getMismatched();
            r.missing = run.getMissing();
            r.matchRate = run.getTotalTxns() > 0
                    ? (double) run.getMatched() / run.getTotalTxns() * 100 : 0;
            r.startedAt = run.getStartedAt();
            r.completedAt = run.getCompletedAt();
            return r;
        }
    }

    @Data
    public static class SettlementUploadRequest {
        @NotNull private String bankRef;
        @NotNull private UUID merchantId;
        @NotNull private BigDecimal amount;
        private String currency = "INR";
        @NotNull private LocalDateTime settledAt;
        private String bankName;
    }
}
