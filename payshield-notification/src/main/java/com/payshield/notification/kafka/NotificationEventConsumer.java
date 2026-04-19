package com.payshield.notification.kafka;

import com.payshield.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "payment.completed", groupId = "payshield-notification")
    public void onPaymentCompleted(Map<String, Object> payload) {
        try {
            String email = getString(payload, "customerEmail");
            if (email == null) return;

            String txnId = getString(payload, "transactionId");
            String amount = getString(payload, "amount");
            String currency = getOrDefault(payload, "currency", "INR");

            notificationService.sendPaymentSuccessEmail(email, txnId, amount, currency);
            log.info("Payment success notification queued for txn {}", txnId);
        } catch (Exception e) {
            log.error("Error handling payment.completed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "payshield-notification")
    public void onPaymentFailed(Map<String, Object> payload) {
        try {
            String email = getString(payload, "customerEmail");
            if (email == null) return;

            String txnId = getString(payload, "transactionId");
            String reason = getOrDefault(payload, "reason", "Payment processing failed");

            notificationService.sendPaymentFailedEmail(email, txnId, reason);
        } catch (Exception e) {
            log.error("Error handling payment.failed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "fraud.alert", groupId = "payshield-notification")
    public void onFraudAlert(Map<String, Object> payload) {
        try {
            String email = getString(payload, "merchantEmail");
            if (email == null) return;

            String txnId = getString(payload, "transactionId");
            double score = payload.get("score") != null
                    ? Double.parseDouble(payload.get("score").toString()) : 0;

            notificationService.sendFraudAlertEmail(email, txnId, score);
        } catch (Exception e) {
            log.error("Error handling fraud.alert event: {}", e.getMessage());
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String getOrDefault(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
