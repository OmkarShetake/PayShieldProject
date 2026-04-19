package com.payshield.reporting.repository;

import com.payshield.reporting.entity.TransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionSummaryRepository extends JpaRepository<TransactionSummary, UUID> {
    Optional<TransactionSummary> findByMerchantIdAndSummaryDate(UUID merchantId, LocalDate date);
    List<TransactionSummary> findByMerchantIdAndSummaryDateAfterOrderBySummaryDateDesc(
            UUID merchantId, LocalDate from);
}
