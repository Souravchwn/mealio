package com.mealio.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily meal log for a member — tracks Breakfast/Lunch/Dinner on/off and guest
 * count.
 * Unique per member per day.
 */
@Entity
@Table(name = "daily_log", uniqueConstraints = @UniqueConstraint(columnNames = { "member_id", "date" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Optimistic Concurrency Control version.
     * Hibernate increments this on every UPDATE. Two concurrent saves on the
     * same row will throw OptimisticLockingFailureException on the second commit,
     * preventing a lost /on or /off toggle.
     */
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    @Builder.Default
    private boolean breakfast = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean lunch = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean dinner = true;

    /** Number of extra guests eating today. */
    @Column(name = "guest_count", nullable = false)
    @Builder.Default
    private int guestCount = 0;

    /** True once the month has been closed — prevents further edits. */
    @Column(nullable = false)
    @Builder.Default
    private boolean frozen = false;
}
