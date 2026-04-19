package com.payshield.reporting.controller;

import com.payshield.reporting.dto.ReportDTOs.*;
import com.payshield.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(reportingService.getDashboard(merchantId, days));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodStat>> getPaymentMethodBreakdown(
            @RequestParam UUID merchantId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(reportingService.getPaymentMethodBreakdown(merchantId, days));
    }
}
