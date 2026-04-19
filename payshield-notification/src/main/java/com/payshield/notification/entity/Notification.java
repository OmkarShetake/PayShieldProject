package com.payshield.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "template_name")
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum NotificationType  { EMAIL, SMS, WEBHOOK, PUSH }
    public enum NotificationStatus { PENDING, SENT, FAILED, RETRYING }
}
