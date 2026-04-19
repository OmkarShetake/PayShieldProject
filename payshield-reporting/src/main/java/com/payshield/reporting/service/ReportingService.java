package com.payshield.reporting.service;

import com.payshield.reporting.dto.ReportDTOs.*;
import com.payshield.reporting.entity.PaymentMethodStats;
import com.payshield.reporting.entity.TransactionSummary;
import com.payshield.reporting.repository.PaymentMethodStatsRepository;
import com.payshield.reporting.repository.TransactionSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final TransactionSummaryRepository summaryRepository;
    private final PaymentMethodStatsRepository pmStatsRepository;

    @Cacheable(value = "dashboard", key = "#merchantId + '_' + #days")
    public DashboardResponse getDashboard(UUID merchantId, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<TransactionSummary> summaries = summaryRepository
                .findByMerchantIdAndSummaryDateAfterOrderBySummaryDateDesc(merchantId, from);

        long totalTxns  = summaries.stream().mapToLong(TransactionSummary::getTotalTransactions).sum();
        long successful = summaries.stream().mapToLong(TransactionSummary::getSuccessful).sum();
        long failed     = summaries.stream().mapToLong(TransactionSummary::getFailed).sum();
        long fraudFlagged = summaries.stream().mapToLong(TransactionSummary::getFlaggedFraud).sum();
        BigDecimal totalVolume = summaries.stream()
                .map(TransactionSummary::getTotalVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgTxn = totalTxns > 0
                ? totalVolume.divide(BigDecimal.valueOf(totalTxns), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double successRate = totalTxns > 0 ? (double) successful / totalTxns * 100 : 0;

        List<DailyVolume> dailyVolumes = summaries.stream()
                .map(s -> new DailyVolume(
                        s.getSummaryDate().toString(),
                        s.getTotalVolume(),
                        s.getTotalTransactions()))
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .merchantId(merchantId)
                .periodDays(days)
                .totalTransactions(totalTxns)
                .successfulTransactions(successful)
                .failedTransactions(failed)
                .fraudFlagged(fraudFlagged)
                .totalVolume(totalVolume)
                .avgTransactionValue(avgTxn)
                .successRate(BigDecimal.valueOf(successRate).setScale(2, RoundingMode.HALF_UP))
                .dailyVolumes(dailyVolumes)
                .build();
    }

    @Cacheable(value = "payment-method-stats", key = "#merchantId + '_' + #days")
    public List<PaymentMethodStat> getPaymentMethodBreakdown(UUID merchantId, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        return pmStatsRepository.getBreakdownByMerchant(merchantId, from);
    }

    /**
     * Upserts the daily transaction summary AND payment method stats.
     * Called by Kafka consumer on every payment event.
     * Cache is evicted so next read reflects fresh data.
     */
    @Transactional
    @CacheEvict(value = {"dashboard", "payment-method-stats"}, key = "#merchantId + '_*'", allEntries = true)
    public void upsertDailySummary(UUID merchantId, LocalDate date,
                                   boolean success, boolean failed, boolean fraud,
                                   BigDecimal amount, String paymentMethod) {

        // ── 1. Update TransactionSummary ─────────────────────────────────────
        TransactionSummary summary = summaryRepository
                .findByMerchantIdAndSummaryDate(merchantId, date)
                .orElse(TransactionSummary.builder()
                        .merchantId(merchantId)
                        .summaryDate(date)
                        .build());

        summary.setTotalTransactions(summary.getTotalTransactions() + 1);
        if (success) summary.setSuccessful(summary.getSuccessful() + 1);
        if (failed)  summary.setFailed(summary.getFailed() + 1);
        if (fraud)   summary.setFlaggedFraud(summary.getFlaggedFraud() + 1);
        summary.setTotalVolume(summary.getTotalVolume().add(amount));

        long total = summary.getTotalTransactions();
        if (total > 0) {
            summary.setAvgTransaction(
                    summary.getTotalVolume().divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            summary.setSuccessRate(
                    BigDecimal.valueOf((double) summary.getSuccessful() / total * 100)
                            .setScale(2, RoundingMode.HALF_UP));
        }
        summaryRepository.save(summary);

        // ── 2. Update PaymentMethodStats ─────────────────────────────────────
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            // Find existing row for this merchant + date + paymentMethod
            Optional<PaymentMethodStats> existingOpt = pmStatsRepository
                    .findByMerchantIdAndSummaryDateAndPaymentMethod(merchantId, date, paymentMethod);

            PaymentMethodStats pmStats = existingOpt.orElse(
                    PaymentMethodStats.builder()
                            .merchantId(merchantId)
                            .summaryDate(date)
                            .paymentMethod(paymentMethod)
                            .build());

            pmStats.setCount(pmStats.getCount() + 1);
            pmStats.setVolume(pmStats.getVolume().add(amount));
            pmStatsRepository.save(pmStats);
        }

        log.debug("Updated daily summary + PM stats for merchant={} date={} method={}", merchantId, date, paymentMethod);
    }
}
