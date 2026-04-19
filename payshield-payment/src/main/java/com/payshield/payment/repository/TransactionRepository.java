package com.payshield.payment.repository;

import com.payshield.payment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByMerchantId(UUID merchantId, Pageable pageable);

    Optional<Transaction> findByIdAndMerchantId(UUID id, UUID merchantId);

    Page<Transaction> findByMerchantIdAndStatus(UUID merchantId,
            Transaction.TransactionStatus status, Pageable pageable);

    List<Transaction> findByFraudFlaggedTrue();

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.merchant.id = :merchantId
        AND t.initiatedAt BETWEEN :from AND :to
        ORDER BY t.initiatedAt DESC
    """)
    List<Transaction> findByMerchantAndDateRange(
            @Param("merchantId") UUID merchantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT SUM(t.amount) FROM Transaction t
        WHERE t.merchant.id = :merchantId
        AND t.status = 'COMPLETED'
        AND t.initiatedAt >= :since
    """)
    BigDecimal sumCompletedAmountSince(@Param("merchantId") UUID merchantId,
                                       @Param("since") LocalDateTime since);

    @Query("""
        SELECT COUNT(t) FROM Transaction t
        WHERE t.merchant.id = :merchantId
        AND t.fraudFlagged = TRUE
        AND t.initiatedAt >= :since
    """)
    long countFlaggedSince(@Param("merchantId") UUID merchantId,
                            @Param("since") LocalDateTime since);
}
