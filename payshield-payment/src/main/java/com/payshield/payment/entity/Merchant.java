package com.payshield.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(nullable = false)
    private String tier = "STANDARD";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
