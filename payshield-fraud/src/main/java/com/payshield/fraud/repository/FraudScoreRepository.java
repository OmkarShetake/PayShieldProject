package com.payshield.fraud.repository;

import com.payshield.fraud.entity.FraudScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudScoreRepository extends JpaRepository<FraudScore, UUID> {
    Optional<FraudScore> findByTransactionId(UUID transactionId);
    List<FraudScore> findByMerchantIdAndFlaggedTrue(UUID merchantId);

    @Query("SELECT f FROM FraudScore f WHERE f.merchantId = :merchantId AND f.createdAt >= :since ORDER BY f.score DESC")
    List<FraudScore> findTopRiskyByMerchant(@Param("merchantId") UUID merchantId,
                                             @Param("since") LocalDateTime since);

    @Query("SELECT AVG(f.score) FROM FraudScore f WHERE f.merchantId = :merchantId AND f.createdAt >= :since")
    Double avgScoreByMerchantSince(@Param("merchantId") UUID merchantId,
                                    @Param("since") LocalDateTime since);
}
