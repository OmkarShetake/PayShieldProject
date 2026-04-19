package com.payshield.reconciliation.controller;

import com.payshield.reconciliation.dto.ReconDTOs.*;
import com.payshield.reconciliation.entity.ReconRecord;
import com.payshield.reconciliation.entity.Settlement;
import com.payshield.reconciliation.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    public ResponseEntity<ReconRunResponse> triggerRun(@Valid @RequestBody ReconRequest request) {
        return ResponseEntity.ok(reconciliationService.triggerReconciliation(request));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ReconRunResponse> getRunReport(@PathVariable UUID runId) {
        return ResponseEntity.ok(reconciliationService.getRunReport(runId));
    }

    @GetMapping("/runs/{runId}/mismatches")
    public ResponseEntity<List<ReconRecord>> getMismatches(@PathVariable UUID runId) {
        return ResponseEntity.ok(reconciliationService.getMismatches(runId));
    }

    @PostMapping("/records/{recordId}/resolve")
    public ResponseEntity<Void> resolveRecord(
            @PathVariable UUID recordId,
            @RequestParam String resolvedBy) {
        reconciliationService.resolveRecord(recordId, resolvedBy);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settlements/upload")
    public ResponseEntity<Settlement> uploadSettlement(
            @Valid @RequestBody SettlementUploadRequest request) {
        return ResponseEntity.ok(reconciliationService.uploadSettlement(request));
    }
}
