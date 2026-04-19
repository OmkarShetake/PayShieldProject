package com.payshield.fraud.service;

import com.payshield.fraud.dto.FraudDTOs.*;
import com.payshield.fraud.entity.FraudScore;
import com.payshield.fraud.repository.FraudScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudScoringEngine {

    private final FraudScoreRepository fraudScoreRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${fraud.rules.high-amount-threshold}")
    private BigDecimal highAmountThreshold;

    @Value("${fraud.rules.velocity-max-per-hour}")
    private int velocityMaxPerHour;

    @Value("${fraud.rules.velocity-max-amount-per-hour}")
    private BigDecimal velocityMaxAmountPerHour;

    @Value("${fraud.rules.flag-score-threshold}")
    private double flagScoreThreshold;

    @Value("${ai.scorer.url}")
    private String aiScorerUrl;

    @Transactional
    public FraudCheckResult evaluateTransaction(FraudCheckRequest request) {
        log.info("Evaluating fraud for transaction: {}", request.getTransactionId());

        List<String> triggeredRules = new ArrayList<>();
        Map<String, Object> features = new HashMap<>();
        double totalScore = 0.0;

        // Rule 1: High amount check
        if (request.getAmount().compareTo(highAmountThreshold) > 0) {
            triggeredRules.add("HIGH_AMOUNT");
            totalScore += 30.0;
            features.put("amount_flag", true);
        }

        // Rule 2: Velocity check (Redis-based sliding window)
        String velocityKey = "velocity:count:" + request.getMerchantId();
        Long txnCount = redisTemplate.opsForValue().increment(velocityKey);
        redisTemplate.expire(velocityKey, 1, TimeUnit.HOURS);
        features.put("hourly_txn_count", txnCount);

        if (txnCount != null && txnCount > velocityMaxPerHour) {
            triggeredRules.add("VELOCITY_COUNT");
            totalScore += 40.0;
        }

        // Rule 3: Amount velocity
        String amtVelocityKey = "velocity:amount:" + request.getMerchantId();
        Object cachedAmt = redisTemplate.opsForValue().get(amtVelocityKey);
        String currentAmtStr = cachedAmt != null ? cachedAmt.toString() : null;
        BigDecimal currentAmt = currentAmtStr != null ? new BigDecimal(currentAmtStr) : BigDecimal.ZERO;
        BigDecimal newAmt = currentAmt.add(request.getAmount());
        redisTemplate.opsForValue().set(amtVelocityKey, newAmt.toPlainString(), 1, TimeUnit.HOURS);
        features.put("hourly_amount", newAmt);

        if (newAmt.compareTo(velocityMaxAmountPerHour) > 0) {
            triggeredRules.add("VELOCITY_AMOUNT");
            totalScore += 35.0;
        }

        // Rule 4: Odd-hours transaction (2am–5am)
        int hour = LocalTime.now().getHour();
        features.put("transaction_hour", hour);
        if (hour >= 2 && hour <= 5) {
            triggeredRules.add("NIGHT_TRANSACTION");
            totalScore += 15.0;
        }

        // Rule 5: Round amount pattern
        if (isRoundAmount(request.getAmount())) {
            triggeredRules.add("ROUND_AMOUNT");
            totalScore += 10.0;
        }

        // Rule 6: Customer email domain check
        if (request.getCustomerEmail() != null) {
            String domain = request.getCustomerEmail().split("@")[1];
            features.put("email_domain", domain);
            if (isSuspiciousDomain(domain)) {
                triggeredRules.add("SUSPICIOUS_EMAIL_DOMAIN");
                totalScore += 20.0;
            }
        }

        // AI model score (call Python FastAPI service)
        double aiScore = getAiScore(request, features);
        features.put("ai_score", aiScore);
        // Blend: 60% rules, 40% AI
        totalScore = (totalScore * 0.6) + (aiScore * 0.4);
        totalScore = Math.min(totalScore, 100.0);

        BigDecimal finalScore = BigDecimal.valueOf(totalScore).setScale(2, java.math.RoundingMode.HALF_UP);
        boolean flagged = totalScore >= flagScoreThreshold;
        String decision = totalScore >= 90 ? "REJECT" : flagged ? "FLAG" : "APPROVE";

        FraudScore fraudScore = FraudScore.builder()
                .transactionId(request.getTransactionId())
                .merchantId(request.getMerchantId())
                .score(finalScore)
                .flagged(flagged)
                .features(features)
                .ruleTriggers(triggeredRules)
                .decision(decision)
                .build();

        fraudScoreRepository.save(fraudScore);

        log.info("Fraud evaluation complete: txn={} score={} decision={} rules={}",
                request.getTransactionId(), finalScore, decision, triggeredRules);

        return FraudCheckResult.builder()
                .transactionId(request.getTransactionId())
                .score(finalScore)
                .flagged(flagged)
                .decision(decision)
                .triggeredRules(triggeredRules)
                .build();
    }

    private double getAiScore(FraudCheckRequest request, Map<String, Object> features) {
        try {
            AiScorerRequest aiRequest = new AiScorerRequest();
            aiRequest.setTransactionId(request.getTransactionId().toString());
            aiRequest.setAmount(request.getAmount().doubleValue());
            aiRequest.setPaymentMethod(request.getPaymentMethod());
            aiRequest.setHour(LocalTime.now().getHour());
            aiRequest.setFeatures(features);

            AiScorerResponse response = restTemplate.postForObject(
                    aiScorerUrl + "/score", aiRequest, AiScorerResponse.class);

            return response != null ? response.getScore() : 0.0;
        } catch (Exception e) {
            log.warn("AI scorer unavailable, using rules only: {}", e.getMessage());
            return 0.0;
        }
    }

    private boolean isRoundAmount(BigDecimal amount) {
        return amount.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0
                && amount.compareTo(BigDecimal.valueOf(5000)) >= 0;
    }

    private boolean isSuspiciousDomain(String domain) {
        Set<String> suspiciousDomains = Set.of("tempmail.com", "throwaway.email",
                "mailinator.com", "guerrillamail.com", "yopmail.com");
        return suspiciousDomains.contains(domain.toLowerCase());
    }
}
