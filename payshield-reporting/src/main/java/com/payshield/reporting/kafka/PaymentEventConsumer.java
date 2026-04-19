package com.payshield.reporting.kafka;

import com.payshield.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes payment events from Kafka and updates daily reporting summaries.
 * This is how the reporting service stays in sync with payment activity
 * without directly coupling to the payment database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ReportingService reportingService;

    @KafkaListener(topics = "payment.completed", groupId = "payshield-reporting")
    public void onPaymentCompleted(Map<String, Object> payload) {
        try {
            UUID merchantId = UUID.fromString(payload.get("merchantId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String paymentMethod = payload.getOrDefault("paymentMethod", "CARD").toString();

            reportingService.upsertDailySummary(
                    merchantId, LocalDate.now(),
                    true, false, false,
                    amount, paymentMethod);

            log.debug("Reporting summary updated for completed payment: merchant={}", merchantId);
        } catch (Exception e) {
            log.error("Error processing payment.completed for reporting: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "payshield-reporting")
    public void onPaymentFailed(Map<String, Object> payload) {
        try {
            UUID merchantId = UUID.fromString(payload.get("merchantId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String paymentMethod = payload.getOrDefault("paymentMethod", "CARD").toString();

            // Check if this was fraud-flagged
            boolean fraudFlagged = Boolean.parseBoolean(
                    payload.getOrDefault("fraudFlagged", "false").toString());

            reportingService.upsertDailySummary(
                    merchantId, LocalDate.now(),
                    false, true, fraudFlagged,
                    amount, paymentMethod);

            log.debug("Reporting summary updated for failed payment: merchant={}", merchantId);
        } catch (Exception e) {
            log.error("Error processing payment.failed for reporting: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.initiated", groupId = "payshield-reporting")
    public void onPaymentInitiated(Map<String, Object> payload) {
        try {
            UUID merchantId = UUID.fromString(payload.get("merchantId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String paymentMethod = payload.getOrDefault("paymentMethod", "CARD").toString();

            // Count initiated but mark as neither success nor fail yet
            reportingService.upsertDailySummary(
                    merchantId, LocalDate.now(),
                    false, false, false,
                    amount, paymentMethod);
        } catch (Exception e) {
            log.error("Error processing payment.initiated for reporting: {}", e.getMessage(), e);
        }
    }
}
