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
import java.util.stream.Collectors;

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

        // Build map of settlements by bank_ref for O(1) lookup
        Map<String, Settlement> settlementMap = settlements.stream()
                .collect(Collectors.toMap(Settlement::getBankRef, s -> s));

        // For each settlement, try to find matching transaction
        for (Settlement settlement : settlements) {
            // In production: look up transaction in payshield-payment DB via REST or shared read replica
            // Here we simulate: if bankRef starts with "MATCH" -> found, else -> missing
            boolean txnFound = !settlement.getBankRef().startsWith("MISS");

            if (txnFound) {
                // Simulate transaction amount (would be fetched from payment DB)
                BigDecimal txnAmount = settlement.getAmount();
                BigDecimal delta = txnAmount.subtract(settlement.getAmount()).abs();
                ReconStatus status = delta.compareTo(amountTolerance) <= 0
                        ? ReconStatus.MATCHED : ReconStatus.MISMATCH;

                records.add(ReconRecord.builder()
                        .merchantId(run.getMerchantId())
                        .settlement(settlement)
                        .txnAmount(txnAmount)
                        .settlementAmount(settlement.getAmount())
                        .delta(delta)
                        .status(status)
                        .mismatchReason(status == ReconStatus.MISMATCH
                                ? "Amount delta of " + delta + " exceeds tolerance" : null)
                        .runId(run.getId())
                        .build());
            } else {
                records.add(ReconRecord.builder()
                        .merchantId(run.getMerchantId())
                        .settlement(settlement)
                        .settlementAmount(settlement.getAmount())
                        .delta(settlement.getAmount())
                        .status(ReconStatus.MISSING)
                        .mismatchReason("No matching transaction found in platform records")
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
