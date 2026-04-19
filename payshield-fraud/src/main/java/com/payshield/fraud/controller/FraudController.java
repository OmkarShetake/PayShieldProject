package com.payshield.fraud.controller;

import com.payshield.fraud.dto.FraudDTOs.*;
import com.payshield.fraud.entity.FraudScore;
import com.payshield.fraud.repository.FraudScoreRepository;
import com.payshield.fraud.service.FraudScoringEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudScoringEngine scoringEngine;
    private final FraudScoreRepository fraudScoreRepository;

    // Manual fraud score request (for testing / admin)
    @PostMapping("/score")
    public ResponseEntity<FraudCheckResult> score(@RequestBody FraudCheckRequest request) {
        return ResponseEntity.ok(scoringEngine.evaluateTransaction(request));
    }

    // Get fraud score for a specific transaction
    @GetMapping("/scores/{transactionId}")
    public ResponseEntity<FraudScore> getScore(@PathVariable UUID transactionId) {
        return fraudScoreRepository.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get all flagged transactions for a merchant
    @GetMapping("/alerts")
    public ResponseEntity<List<FraudScore>> getFlaggedTransactions(
            @RequestParam UUID merchantId) {
        return ResponseEntity.ok(fraudScoreRepository.findByMerchantIdAndFlaggedTrue(merchantId));
    }

    // Get top risky transactions in last N hours
    @GetMapping("/risky")
    public ResponseEntity<List<FraudScore>> getRiskyTransactions(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(fraudScoreRepository.findTopRiskyByMerchant(merchantId, since));
    }

    // Average fraud score for merchant
    @GetMapping("/avg-score")
    public ResponseEntity<Double> getAvgScore(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "24") int hours) {
        Double avg = fraudScoreRepository.avgScoreByMerchantSince(
                merchantId, LocalDateTime.now().minusHours(hours));
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }
}
