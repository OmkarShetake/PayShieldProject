package com.payshield.payment.kafka;

import com.payshield.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes fraud check results from the payshield-fraud
 * and updates the transaction's fraud score + status in the payment DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FraudCheckResponseConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${kafka.topics.fraud-check-response}",
            groupId = "payshield-payment-fraud-response"
    )
    public void handleFraudCheckResponse(Map<String, Object> payload) {
        try {
            String txnIdStr = payload.get("transactionId") != null
                    ? payload.get("transactionId").toString() : null;
            if (txnIdStr == null) {
                log.warn("Received fraud response with null transactionId");
                return;
            }

            UUID transactionId = UUID.fromString(txnIdStr);
            BigDecimal score = new BigDecimal(payload.get("score").toString());
            boolean flagged = Boolean.parseBoolean(payload.get("flagged").toString());
            String decision = payload.getOrDefault("decision", "APPROVE").toString();

            log.info("Fraud result received: txn={} score={} flagged={} decision={}",
                    transactionId, score, flagged, decision);

            paymentService.updateFraudScore(transactionId, score, flagged);

        } catch (Exception e) {
            log.error("Error processing fraud check response: {}", e.getMessage(), e);
        }
    }
}
