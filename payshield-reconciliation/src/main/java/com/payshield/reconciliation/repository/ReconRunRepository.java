package com.payshield.reconciliation.repository;

import com.payshield.reconciliation.entity.ReconRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReconRunRepository extends JpaRepository<ReconRun, UUID> {
    List<ReconRun> findByMerchantIdOrderByStartedAtDesc(UUID merchantId);
}
