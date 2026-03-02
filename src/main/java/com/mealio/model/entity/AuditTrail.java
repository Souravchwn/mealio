package com.mealio.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin correction audit trail — every manual edit to DailyLog or Expense is
 * recorded here.
 * Ensures data integrity and trust verification (SRS NFR).
 */
@Entity
@Table(name = "audit_trail", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Admin who performed the correction. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;

    /** e.g. "DailyLog", "Expense" */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** UUID of the corrected record. */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    /** JSON snapshot of the record before correction. */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** JSON snapshot of the record after correction. */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null)
            createdAt = Instant.now();
    }
}
