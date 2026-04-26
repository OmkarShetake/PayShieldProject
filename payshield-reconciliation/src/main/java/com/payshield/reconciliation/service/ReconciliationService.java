package com.payshield.reconciliation.service;

import com.payshield.reconciliation.dto.ReconDTOs.*;
import com.payshield.reconciliation.entity.ReconRecord;
import com.payshield.reconciliation.entity.ReconRecord.ReconStatus;
import com.payshield.reconciliation.entity.ReconRun;
import com.payshield.reconciliation.entity.Settlement;
import com.payshield.reconciliation.repository.ReconRecordRepository;
import com.payshield.reconciliation.repository.ReconRunRepository;
import com.payshield.reconciliation.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final SettlementRepository settlementRepository;
    private final ReconRecordRepository reconRecordRepository;
    private final ReconRunRepository reconRunRepository;

    @Value("${recon.tolerance.amount-delta}")
    private BigDecimal amountTolerance;

    @Transactional
    public ReconRunResponse triggerReconciliation(ReconRequest request) {
        log.info("Starting reconciliation for merchant {} from {} to {}",
                request.getMerchantId(), request.getFromDate(), request.getToDate());

        ReconRun run = ReconRun.builder()
                .merchantId(request.getMerchantId())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .triggeredBy(request.getTriggeredBy())
                .status("RUNNING")
                .build();
        run = reconRunRepository.save(run);

        try {
            // Get settlements from bank (in real world: fetched from bank API/SFTP)
            List<Settlement> settlements = settlementRepository
                    .findByMerchantIdAndSettledAtBetween(
                            request.getMerchantId(),
                            request.getFromDate(),
                            request.getToDate());

            // Get our transaction records (in real world: fetched from payshield-payment)
            // Simulated here as the settlement data is our source of truth for demo
            List<ReconRecord> records = performMatching(settlements, run);

            reconRecordRepository.saveAll(records);

            long matched = records.stream().filter(r -> r.getStatus() == ReconStatus.MATCHED).count();
            long mismatched = records.stream().filter(r -> r.getStatus() == ReconStatus.MISMATCH).count();
            long missing = records.stream().filter(r -> r.getStatus() == ReconStatus.MISSING).count();

            run.setTotalTxns(records.size());
            run.setMatched((int) matched);
            run.setMismatched((int) mismatched);
            run.setMissing((int) missing);
            run.setStatus("COMPLETED");
            run.setCompletedAt(LocalDateTime.now());
            reconRunRepository.save(run);

            log.info("Reconciliation complete: runId={} matched={} mismatched={} missing={}",
                    run.getId(), matched, mismatched, missing);

            return ReconRunResponse.from(run);

        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setCompletedAt(LocalDateTime.now());
            reconRunRepository.save(run);
            log.error("Reconciliation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Reconciliation failed: " + e.getMessage());
        }
    }

    private List<ReconRecord> performMatching(List<Settlement> settlements, ReconRun run) {
        List<ReconRecord> records = new ArrayList<>();

        for (Settlement settlement : settlements) {
            /*
             * Matching logic:
             * In production this would call payshield-payment via REST or a read replica
             * to look up the transaction by bankRef or amount+date.
             *
             * Here we simulate realistic outcomes:
             *  - 94% of settlements match exactly
             *  - 4% have a small amount delta (fee deduction, rounding)
             *  - 2% are missing (no corresponding transaction found)
             *
             * The outcome is deterministic per bankRef so repeated runs are consistent.
             */
            int hash = Math.abs(settlement.getBankRef().hashCode()) % 100;

            if (hash < 2) {
                // MISSING — no transaction found for this settlement
                records.add(ReconRecord.builder()
                        .merchantId(run.getMerchantId())
                        .settlement(settlement)
                        .settlementAmount(settlement.getAmount())
                        .delta(settlement.getAmount())
                        .status(ReconStatus.MISSING)
                        .mismatchReason("No matching transaction found in platform records")
                        .runId(run.getId())
                        .build());

            } else if (hash < 6) {
                // MISMATCH — small delta (e.g. payment gateway fee deducted)
                BigDecimal fee = BigDecimal.valueOf(hash % 3 == 0 ? 18.0 : 9.0); // simulate GST/fee
                BigDecimal txnAmount = settlement.getAmount().add(fee);
                BigDecimal delta = txnAmount.subtract(settlement.getAmount()).abs();

                records.add(ReconRecord.builder()
                        .merchantId(run.getMerchantId())
                        .settlement(settlement)
                        .txnAmount(txnAmount)
                        .settlementAmount(settlement.getAmount())
                        .delta(delta)
                        .status(ReconStatus.MISMATCH)
                        .mismatchReason("Amount delta ₹" + delta + " — possible gateway fee deduction")
                        .runId(run.getId())
                        .build());

            } else {
                // MATCHED — amounts agree within tolerance
                records.add(ReconRecord.builder()
                        .merchantId(run.getMerchantId())
                        .settlement(settlement)
                        .txnAmount(settlement.getAmount())
                        .settlementAmount(settlement.getAmount())
                        .delta(BigDecimal.ZERO)
                        .status(ReconStatus.MATCHED)
                        .runId(run.getId())
                        .build());
            }
        }

        return records;
    }

    public ReconRunResponse getRunReport(UUID runId) {
        ReconRun run = reconRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("Run not found: " + runId));
        return ReconRunResponse.from(run);
    }

    public List<ReconRecord> getMismatches(UUID runId) {
        return reconRecordRepository.findByRunIdAndStatusNot(runId, ReconStatus.MATCHED);
    }

    @Transactional
    public void resolveRecord(UUID recordId, String resolvedBy) {
        ReconRecord record = reconRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        record.setResolved(true);
        record.setResolvedAt(LocalDateTime.now());
        record.setResolvedBy(resolvedBy);
        reconRecordRepository.save(record);
    }

    @Transactional
    public Settlement uploadSettlement(SettlementUploadRequest request) {
        Settlement settlement = Settlement.builder()
                .bankRef(request.getBankRef())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .settledAt(request.getSettledAt())
                .bankName(request.getBankName())
                .build();
        return settlementRepository.save(settlement);
    }
}
