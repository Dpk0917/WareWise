package com.app.warewise.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String action;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String type;

    private Long recordId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime date;

    private Long batchId;

    @PrePersist
    protected void onCreate() {
        if (date == null) {
            date = LocalDateTime.now();
        }
    }
}
