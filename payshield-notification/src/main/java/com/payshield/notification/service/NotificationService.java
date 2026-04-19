package com.payshield.notification.service;

import com.payshield.notification.entity.Notification;
import com.payshield.notification.entity.Notification.NotificationStatus;
import com.payshield.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Value("${notification.from-name}")
    private String fromName;

    @Async
    public void sendPaymentSuccessEmail(String toEmail, String txnId, String amount, String currency) {
        String subject = "Payment Successful - " + txnId;
        String body = buildPaymentSuccessBody(txnId, amount, currency);
        sendEmail(toEmail, subject, body, txnId, "TRANSACTION");
    }

    @Async
    public void sendFraudAlertEmail(String toEmail, String txnId, double fraudScore) {
        String subject = "⚠️ Fraud Alert - Transaction " + txnId;
        String body = buildFraudAlertBody(txnId, fraudScore);
        sendEmail(toEmail, subject, body, txnId, "FRAUD_ALERT");
    }

    @Async
    public void sendPaymentFailedEmail(String toEmail, String txnId, String reason) {
        String subject = "Payment Failed - " + txnId;
        String body = buildPaymentFailedBody(txnId, reason);
        sendEmail(toEmail, subject, body, txnId, "TRANSACTION");
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendWebhook(String webhookUrl, Map<String, Object> payload, String txnId) {
        try {
            log.info("Sending webhook to {} for txn {}", webhookUrl, txnId);
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("Webhook delivered to {} for txn {}", webhookUrl, txnId);

            Notification notification = Notification.builder()
                    .type(Notification.NotificationType.WEBHOOK)
                    .recipient(webhookUrl)
                    .body(payload.toString())
                    .status(NotificationStatus.SENT)
                    .referenceId(txnId)
                    .referenceType("TRANSACTION")
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Webhook delivery failed to {}: {}", webhookUrl, e.getMessage());
            throw e; // trigger retry
        }
    }

    private void sendEmail(String to, String subject, String body,
                           String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .type(Notification.NotificationType.EMAIL)
                .recipient(to)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notification = notificationRepository.save(notification);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            mailSender.send(message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setAttempts(1);
            notificationRepository.save(notification);
            log.info("Email sent to {} for ref {}", to, referenceId);

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setAttempts(notification.getAttempts() + 1);
            notificationRepository.save(notification);
            log.error("Email failed to {}: {}", to, e.getMessage());
        }
    }

    private String buildPaymentSuccessBody(String txnId, String amount, String currency) {
        return """
            <html><body style="font-family:sans-serif;max-width:600px;margin:0 auto">
              <div style="background:#1a1a2e;padding:24px;border-radius:8px">
                <h2 style="color:#00d4aa;margin:0">✓ Payment Successful</h2>
              </div>
              <div style="padding:24px;border:1px solid #eee;border-radius:8px;margin-top:8px">
                <p>Your payment has been processed successfully.</p>
                <table style="width:100%;border-collapse:collapse">
                  <tr><td style="padding:8px;color:#666">Transaction ID</td>
                      <td style="padding:8px;font-weight:bold">%s</td></tr>
                  <tr><td style="padding:8px;color:#666">Amount</td>
                      <td style="padding:8px;font-weight:bold;color:#00a878">%s %s</td></tr>
                </table>
                <p style="color:#666;font-size:12px;margin-top:24px">PayShield Payment Platform</p>
              </div>
            </body></html>
            """.formatted(txnId, currency, amount);
    }

    private String buildFraudAlertBody(String txnId, double score) {
        return """
            <html><body style="font-family:sans-serif;max-width:600px;margin:0 auto">
              <div style="background:#c0392b;padding:24px;border-radius:8px">
                <h2 style="color:#fff;margin:0">⚠️ Fraud Alert</h2>
              </div>
              <div style="padding:24px;border:1px solid #eee;border-radius:8px;margin-top:8px">
                <p>A transaction has been flagged for potential fraud.</p>
                <table style="width:100%;border-collapse:collapse">
                  <tr><td style="padding:8px;color:#666">Transaction ID</td>
                      <td style="padding:8px;font-weight:bold">%s</td></tr>
                  <tr><td style="padding:8px;color:#666">Fraud Score</td>
                      <td style="padding:8px;font-weight:bold;color:#c0392b">%.1f / 100</td></tr>
                </table>
                <p>Please review this transaction in your dashboard immediately.</p>
              </div>
            </body></html>
            """.formatted(txnId, score);
    }

    private String buildPaymentFailedBody(String txnId, String reason) {
        return """
            <html><body style="font-family:sans-serif;max-width:600px;margin:0 auto">
              <div style="background:#e67e22;padding:24px;border-radius:8px">
                <h2 style="color:#fff;margin:0">✗ Payment Failed</h2>
              </div>
              <div style="padding:24px;border:1px solid #eee;border-radius:8px;margin-top:8px">
                <p>Unfortunately, your payment could not be processed.</p>
                <table style="width:100%;border-collapse:collapse">
                  <tr><td style="padding:8px;color:#666">Transaction ID</td>
                      <td style="padding:8px;font-weight:bold">%s</td></tr>
                  <tr><td style="padding:8px;color:#666">Reason</td>
                      <td style="padding:8px;color:#e67e22">%s</td></tr>
                </table>
                <p>Please try again or contact support.</p>
              </div>
            </body></html>
            """.formatted(txnId, reason);
    }
}
