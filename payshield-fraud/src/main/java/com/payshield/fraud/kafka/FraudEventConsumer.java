package com.payshield.fraud.kafka;

import com.payshield.fraud.dto.FraudDTOs.*;
import com.payshield.fraud.service.FraudScoringEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventConsumer {

    private final FraudScoringEngine scoringEngine;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.fraud-check-response}")
    private String fraudCheckResponseTopic;

    @KafkaListener(topics = "${kafka.topics.fraud-check-request}",
                   groupId = "payshield-fraud")
    public void handleFraudCheckRequest(Map<String, Object> payload) {
        try {
            log.info("Received fraud check request: {}", payload.get("transactionId"));

            FraudCheckRequest request = new FraudCheckRequest();
            request.setTransactionId(java.util.UUID.fromString(payload.get("transactionId").toString()));
            request.setMerchantId(java.util.UUID.fromString(payload.get("merchantId").toString()));
            request.setAmount(new java.math.BigDecimal(payload.get("amount").toString()));
            request.setCurrency(payload.getOrDefault("currency", "INR").toString());
            request.setPaymentMethod(payload.getOrDefault("paymentMethod", "CARD").toString());

            if (payload.get("customerEmail") != null) {
                request.setCustomerEmail(payload.get("customerEmail").toString());
            }
            if (payload.get("customerPhone") != null) {
                request.setCustomerPhone(payload.get("customerPhone").toString());
            }

            FraudCheckResult result = scoringEngine.evaluateTransaction(request);

            // Publish result back → payment service listens and updates txn
            kafkaTemplate.send(fraudCheckResponseTopic,
                    result.getTransactionId().toString(),
                    Map.of(
                            "transactionId", result.getTransactionId().toString(),
                            "score", result.getScore(),
                            "flagged", result.isFlagged(),
                            "decision", result.getDecision(),
                            "triggeredRules", result.getTriggeredRules()
                    ));

        } catch (Exception e) {
            log.error("Error processing fraud check request: {}", e.getMessage(), e);
        }
    }
}
