package com.payshield.reconciliation.repository;

import com.payshield.reconciliation.entity.ReconRecord;
import com.payshield.reconciliation.entity.ReconRecord.ReconStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconRecordRepository extends JpaRepository<ReconRecord, UUID> {
    List<ReconRecord> findByRunId(UUID runId);
    List<ReconRecord> findByRunIdAndStatusNot(UUID runId, ReconStatus status);
    List<ReconRecord> findByMerchantIdAndStatus(UUID merchantId, ReconStatus status);
}
