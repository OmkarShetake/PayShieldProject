package com.payshield.payment.service;

import com.payshield.payment.dto.PaymentDTOs.*;
import com.payshield.payment.entity.Merchant;
import com.payshield.payment.entity.Transaction;
import com.payshield.payment.entity.Transaction.TransactionStatus;
import com.payshield.payment.exception.PaymentException;
import com.payshield.payment.kafka.PaymentEventProducer;
import com.payshield.payment.repository.MerchantRepository;
import com.payshield.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final PaymentEventProducer eventProducer;

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request, UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new PaymentException("Merchant not found"));

        if (!merchant.isActive()) {
            throw new PaymentException("Merchant account is inactive");
        }

        Transaction txn = Transaction.builder()
                .merchant(merchant)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .description(request.getDescription())
                .externalRef(request.getExternalRef())
                .metadata(request.getMetadata())
                .status(TransactionStatus.INITIATED)
                .build();

        txn = transactionRepository.save(txn);

        // Publish to Kafka → triggers fraud check
        PaymentEvent event = buildEvent(txn);
        eventProducer.publishPaymentInitiated(event);
        eventProducer.publishFraudCheckRequest(event);

        // Move to FRAUD_CHECK state
        txn.setStatus(TransactionStatus.FRAUD_CHECK);
        txn = transactionRepository.save(txn);

        log.info("Payment initiated: {} for merchant: {} amount: {}{}",
                txn.getId(), merchantId, txn.getCurrency(), txn.getAmount());

        return PaymentResponse.from(txn);
    }

    @Transactional
    public PaymentResponse processPayment(UUID transactionId, UUID merchantId) {
        Transaction txn = transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                .orElseThrow(() -> new PaymentException("Transaction not found"));

        if (txn.isFraudFlagged()) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            eventProducer.publishPaymentFailed(buildEvent(txn));
            throw new PaymentException("Transaction flagged for fraud. Payment rejected.");
        }

        if (txn.getStatus() != TransactionStatus.FRAUD_CHECK &&
            txn.getStatus() != TransactionStatus.PENDING) {
            throw new PaymentException("Transaction not in processable state: " + txn.getStatus());
        }

        // Simulate gateway processing
        txn.setStatus(TransactionStatus.PROCESSING);
        txn.setGatewayTxnId("GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transactionRepository.save(txn);

        // Simulate success (in real world: call payment gateway here)
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        txn = transactionRepository.save(txn);

        eventProducer.publishPaymentCompleted(buildEvent(txn));

        log.info("Payment completed: {} gateway: {}", txn.getId(), txn.getGatewayTxnId());
        return PaymentResponse.from(txn);
    }

    @Cacheable(value = "transaction", key = "#transactionId")
    public PaymentResponse getTransaction(UUID transactionId, UUID merchantId) {
        Transaction txn = transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
                .orElseThrow(() -> new PaymentException("Transaction not found"));
        return PaymentResponse.from(txn);
    }

    public Page<PaymentResponse> getTransactions(UUID merchantId, int page, int size, String status) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("initiatedAt").descending());

        Page<Transaction> txns;
        if (status != null && !status.isBlank()) {
            TransactionStatus txnStatus = TransactionStatus.valueOf(status.toUpperCase());
            txns = transactionRepository.findByMerchantIdAndStatus(merchantId, txnStatus, pageRequest);
        } else {
            txns = transactionRepository.findByMerchantId(merchantId, pageRequest);
        }

        return txns.map(PaymentResponse::from);
    }

    @Transactional
    public void updateFraudScore(UUID transactionId, BigDecimal score, boolean flagged) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException("Transaction not found"));
        txn.setFraudScore(score);
        txn.setFraudFlagged(flagged);
        if (!flagged) {
            txn.setStatus(TransactionStatus.PENDING);
        }
        transactionRepository.save(txn);
        log.info("Fraud score updated for txn {}: score={} flagged={}", transactionId, score, flagged);
    }

    private PaymentEvent buildEvent(Transaction txn) {
        PaymentEvent event = new PaymentEvent();
        event.setTransactionId(txn.getId());
        event.setMerchantId(txn.getMerchant().getId());
        event.setAmount(txn.getAmount());
        event.setCurrency(txn.getCurrency());
        event.setStatus(txn.getStatus().name());
        event.setPaymentMethod(txn.getPaymentMethod().name());
        event.setCustomerEmail(txn.getCustomerEmail());
        event.setCustomerPhone(txn.getCustomerPhone());
        event.setTimestamp(LocalDateTime.now());
        event.setMetadata(txn.getMetadata());
        event.setFraudFlagged(txn.isFraudFlagged());
        return event;
    }
}
