package com.payshield.reporting.controller;

import com.payshield.reporting.dto.ReportDTOs.*;
import com.payshield.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "Business intelligence — dashboards, volume trends, payment method breakdown")
public class ReportingController {

    private final ReportingService reportingService;

    @Operation(
        summary = "Dashboard Summary",
        description = "Get key metrics for a merchant — total volume, transaction count, success rate, fraud flagged count, and daily volume trend. Cached in Redis for 5 minutes.")
    @ApiResponse(responseCode = "200", description = "Dashboard data returned")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(description = "Merchant UUID", required = true) @RequestParam UUID merchantId,
            @Parameter(description = "Number of days to look back", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(reportingService.getDashboard(merchantId, days));
    }

    @Operation(
        summary = "Payment Method Breakdown",
        description = "Get transaction count and volume split by payment method (UPI, CARD, NET_BANKING, WALLET, BANK_TRANSFER).")
    @ApiResponse(responseCode = "200", description = "Payment method stats returned")
    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodStat>> getPaymentMethodBreakdown(
            @Parameter(description = "Merchant UUID", required = true) @RequestParam UUID merchantId,
            @Parameter(description = "Number of days to look back", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(reportingService.getPaymentMethodBreakdown(merchantId, days));
    }
}
