package com.payshield.reconciliation.controller;

import com.payshield.reconciliation.dto.ReconDTOs.*;
import com.payshield.reconciliation.entity.ReconRecord;
import com.payshield.reconciliation.entity.Settlement;
import com.payshield.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Reconciliation", description = "Settlement matching — run jobs, view mismatches, resolve discrepancies")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Operation(
        summary = "Run Reconciliation",
        description = "Trigger a new reconciliation job for a date range. Matches PayShield transactions against bank settlement records. Returns a run ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run started — returns run ID and summary"),
        @ApiResponse(responseCode = "400", description = "Invalid date range or merchant ID")
    })
    @PostMapping("/run")
    public ResponseEntity<ReconRunResponse> triggerRun(@Valid @RequestBody ReconRequest request) {
        return ResponseEntity.ok(reconciliationService.triggerReconciliation(request));
    }

    @Operation(
        summary = "Get Run Report",
        description = "Retrieve full summary of a reconciliation run — total transactions, matched count, mismatch count, match rate.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Run report found"),
        @ApiResponse(responseCode = "404", description = "Run not found")
    })
    @GetMapping("/runs/{runId}")
    public ResponseEntity<ReconRunResponse> getRunReport(
            @Parameter(description = "Reconciliation run UUID") @PathVariable UUID runId) {
        return ResponseEntity.ok(reconciliationService.getRunReport(runId));
    }

    @Operation(
        summary = "Get Mismatches",
        description = "List all mismatch records for a reconciliation run. Each record shows the delta between PayShield amount and bank settlement amount.")
    @ApiResponse(responseCode = "200", description = "List of mismatch records")
    @GetMapping("/runs/{runId}/mismatches")
    public ResponseEntity<List<ReconRecord>> getMismatches(
            @Parameter(description = "Reconciliation run UUID") @PathVariable UUID runId) {
        return ResponseEntity.ok(reconciliationService.getMismatches(runId));
    }

    @Operation(
        summary = "Resolve Mismatch Record",
        description = "Mark a mismatch record as resolved. Typically done after manual investigation confirms the discrepancy has been handled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Record resolved"),
        @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @PostMapping("/records/{recordId}/resolve")
    public ResponseEntity<Void> resolveRecord(
            @Parameter(description = "Recon record UUID") @PathVariable UUID recordId,
            @Parameter(description = "Email of person resolving", example = "admin@payshield.com")
            @RequestParam String resolvedBy) {
        reconciliationService.resolveRecord(recordId, resolvedBy);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Upload Bank Settlement",
        description = "Upload a bank settlement record to be matched against PayShield transactions during the next reconciliation run.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settlement uploaded"),
        @ApiResponse(responseCode = "409", description = "Bank reference already exists")
    })
    @PostMapping("/settlements/upload")
    public ResponseEntity<Settlement> uploadSettlement(
            @Valid @RequestBody SettlementUploadRequest request) {
        return ResponseEntity.ok(reconciliationService.uploadSettlement(request));
    }
}
