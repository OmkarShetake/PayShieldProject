package com.payshield.payment.kafka;

import com.payshield.payment.dto.PaymentDTOs.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${kafka.topics.payment-initiated}")
    private String paymentInitiatedTopic;

    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    @Value("${kafka.topics.fraud-check-request}")
    private String fraudCheckTopic;

    public void publishPaymentInitiated(PaymentEvent event) {
        publish(paymentInitiatedTopic, event);
    }

    public void publishPaymentCompleted(PaymentEvent event) {
        publish(paymentCompletedTopic, event);
    }

    public void publishPaymentFailed(PaymentEvent event) {
        publish(paymentFailedTopic, event);
    }

    public void publishFraudCheckRequest(PaymentEvent event) {
        publish(fraudCheckTopic, event);
    }

    private void publish(String topic, PaymentEvent event) {
        String key = event.getTransactionId().toString();
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Published event to topic {} partition {} offset {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
