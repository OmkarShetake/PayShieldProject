package com.payshield.notification.controller;

import com.payshield.notification.entity.Notification;
import com.payshield.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "View and manage notification history — emails, webhooks, fraud alerts")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @Operation(summary = "List Notifications", description = "Paginated list of all notifications, newest first.")
    @ApiResponse(responseCode = "200", description = "Notification list returned")
    @GetMapping
    public ResponseEntity<Page<Notification>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
                notificationRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
        );
    }

    @Operation(summary = "Get Notification", description = "Get a single notification by ID.")
    @ApiResponse(responseCode = "200", description = "Notification found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<Notification> get(
            @Parameter(description = "Notification UUID") @PathVariable UUID id) {
        return notificationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get by Reference", description = "Get all notifications for a specific transaction or entity.")
    @ApiResponse(responseCode = "200", description = "Notifications found")
    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<List<Notification>> getByReference(
            @Parameter(description = "Reference ID (e.g. transaction UUID)") @PathVariable String referenceId,
            @Parameter(description = "Reference type (e.g. TRANSACTION, FRAUD_ALERT)") @RequestParam(defaultValue = "TRANSACTION") String referenceType) {
        return ResponseEntity.ok(
                notificationRepository.findByReferenceIdAndReferenceType(referenceId, referenceType)
        );
    }

    @Operation(summary = "Get Failed Notifications", description = "List all notifications that failed to deliver.")
    @ApiResponse(responseCode = "200", description = "Failed notifications returned")
    @GetMapping("/failed")
    public ResponseEntity<List<Notification>> getFailed() {
        return ResponseEntity.ok(notificationRepository.findByStatus(Notification.NotificationStatus.FAILED));
    }
}
