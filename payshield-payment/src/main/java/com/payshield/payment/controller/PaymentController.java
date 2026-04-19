package com.payshield.payment.controller;

import com.payshield.payment.dto.PaymentDTOs.*;
import com.payshield.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // In production, merchantId extracted from JWT principal
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(request, merchantId));
    }

    @PostMapping("/{transactionId}/process")
    public ResponseEntity<PaymentResponse> process(
            @PathVariable UUID transactionId,
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.ok(paymentService.processPayment(transactionId, merchantId));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.ok(paymentService.getTransaction(transactionId, merchantId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> listTransactions(
            @RequestHeader("X-Merchant-Id") UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(paymentService.getTransactions(merchantId, page, size, status));
    }
}
