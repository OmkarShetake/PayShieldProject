package com.payshield.reporting.repository;

import com.payshield.reporting.dto.ReportDTOs.PaymentMethodStat;
import com.payshield.reporting.entity.PaymentMethodStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodStatsRepository extends JpaRepository<PaymentMethodStats, UUID> {

    Optional<PaymentMethodStats> findByMerchantIdAndSummaryDateAndPaymentMethod(
            UUID merchantId, LocalDate summaryDate, String paymentMethod);

    @Query("""
        SELECT p.paymentMethod  as paymentMethod,
               SUM(p.count)    as count,
               SUM(p.volume)   as volume
        FROM PaymentMethodStats p
        WHERE p.merchantId  = :merchantId
          AND p.summaryDate >= :from
        GROUP BY p.paymentMethod
        ORDER BY SUM(p.count) DESC
    """)
    List<PaymentMethodStat> getBreakdownByMerchant(
            @Param("merchantId") UUID merchantId,
            @Param("from") LocalDate from);
}
