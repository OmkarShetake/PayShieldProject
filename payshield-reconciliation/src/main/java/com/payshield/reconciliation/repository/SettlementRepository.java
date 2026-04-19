package com.payshield.reconciliation.repository;

import com.payshield.reconciliation.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByMerchantIdAndSettledAtBetween(UUID merchantId, LocalDateTime from, LocalDateTime to);
}
