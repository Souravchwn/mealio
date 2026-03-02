package com.mealio.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a shared-living group ("mess").
 * The cut-off time controls when members can no longer toggle their meal
 * status.
 */
@Entity
@Table(name = "mess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    /**
     * BD/IN timezone expected — stored as wall-clock time, interpreted in mess
     * local TZ.
     */
    @Column(name = "cut_off_time", nullable = false)
    private LocalTime cutOffTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null)
            createdAt = Instant.now();
        if (cutOffTime == null)
            cutOffTime = LocalTime.of(21, 0); // default 9 PM
    }
}
