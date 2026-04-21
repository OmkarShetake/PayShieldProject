package com.payshield.payment.controller;

import com.payshield.payment.dto.PaymentDTOs.*;
import com.payshield.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Initiate, process, and track payment transactions")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Initiate Payment",
            description = "Create a new payment transaction. Triggers fraud check automatically via Kafka. Status starts as INITIATED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Merchant not found")
    })
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            @Parameter(description = "Merchant UUID", required = true)
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(request, merchantId));
    }

    @Operation(
            summary = "Process Payment",
            description = "Move a payment from INITIATED/FRAUD_CHECK to PROCESSING/COMPLETED. Sends Kafka event on completion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment processed"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Transaction already processed or blocked")
    })
    @PostMapping("/{transactionId}/process")
    public ResponseEntity<PaymentResponse> process(
            @Parameter(description = "Transaction UUID") @PathVariable UUID transactionId,
            @Parameter(description = "Merchant UUID", required = true)
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.ok(paymentService.processPayment(transactionId, merchantId));
    }

    @Operation(
            summary = "Get Transaction",
            description = "Retrieve full details of a specific transaction including fraud score and status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransaction(
            @Parameter(description = "Transaction UUID") @PathVariable UUID transactionId,
            @Parameter(description = "Merchant UUID", required = true)
            @RequestHeader("X-Merchant-Id") UUID merchantId) {
        return ResponseEntity.ok(paymentService.getTransaction(transactionId, merchantId));
    }

    @Operation(
            summary = "List Transactions",
            description = "Paginated list of all transactions for a merchant. Filter by status (COMPLETED, FAILED, PENDING, etc).")
    @ApiResponse(responseCode = "200", description = "Paginated transaction list")
    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> listTransactions(
            @Parameter(description = "Merchant UUID", required = true)
            @RequestHeader("X-Merchant-Id") UUID merchantId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by status: COMPLETED | FAILED | PENDING | FRAUD_CHECK")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(paymentService.getTransactions(merchantId, page, size, status));
    }
}
