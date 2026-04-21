package com.payshield.fraud.controller;

import com.payshield.fraud.dto.FraudDTOs.*;
import com.payshield.fraud.entity.FraudScore;
import com.payshield.fraud.repository.FraudScoreRepository;
import com.payshield.fraud.service.FraudScoringEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Tag(name = "Fraud Detection", description = "AI-powered fraud scoring and alert management")
public class FraudController {

    private final FraudScoringEngine scoringEngine;
    private final FraudScoreRepository fraudScoreRepository;

    @Operation(
            summary = "Score Transaction",
            description = "Manually submit a transaction for fraud analysis. Uses XGBoost model + rule engine. Returns score 0-100 and decision (APPROVE/FLAG/REJECT).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fraud score calculated"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/score")
    public ResponseEntity<FraudCheckResult> score(@RequestBody FraudCheckRequest request) {
        return ResponseEntity.ok(scoringEngine.evaluateTransaction(request));
    }

    @Operation(
            summary = "Get Fraud Score",
            description = "Retrieve the fraud score record for a specific transaction ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score found"),
            @ApiResponse(responseCode = "404", description = "No score for this transaction")
    })
    @GetMapping("/scores/{transactionId}")
    public ResponseEntity<FraudScore> getScore(
            @Parameter(description = "Transaction UUID") @PathVariable UUID transactionId) {
        return fraudScoreRepository.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get Fraud Alerts",
            description = "Get all fraud-flagged transactions for a merchant. These require manual review.")
    @ApiResponse(responseCode = "200", description = "List of flagged transactions")
    @GetMapping("/alerts")
    public ResponseEntity<List<FraudScore>> getFlaggedTransactions(
            @Parameter(description = "Merchant UUID", required = true)
            @RequestParam UUID merchantId) {
        return ResponseEntity.ok(fraudScoreRepository.findByMerchantIdAndFlaggedTrue(merchantId));
    }

    @Operation(
            summary = "Get Risky Transactions",
            description = "Get top high-scoring transactions for a merchant within the last N hours.")
    @ApiResponse(responseCode = "200", description = "List of high-risk transactions sorted by score")
    @GetMapping("/risky")
    public ResponseEntity<List<FraudScore>> getRiskyTransactions(
            @Parameter(description = "Merchant UUID", required = true) @RequestParam UUID merchantId,
            @Parameter(description = "Look-back window in hours", example = "24")
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(fraudScoreRepository.findTopRiskyByMerchant(merchantId, since));
    }

    @Operation(
            summary = "Average Fraud Score",
            description = "Get the average fraud score across all transactions for a merchant in the last N hours.")
    @ApiResponse(responseCode = "200", description = "Average score (0.0 if no data)")
    @GetMapping("/avg-score")
    public ResponseEntity<Double> getAvgScore(
            @Parameter(description = "Merchant UUID", required = true) @RequestParam UUID merchantId,
            @Parameter(description = "Look-back window in hours", example = "24")
            @RequestParam(defaultValue = "24") int hours) {
        Double avg = fraudScoreRepository.avgScoreByMerchantSince(
                merchantId, LocalDateTime.now().minusHours(hours));
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }
}
