package com.mealio.model.entity;

import com.mealio.model.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single bazaar expense entry made by the current Meal Manager.
 * Category is the raw data point for future AI health & economic analysis.
 */
@Entity
@Table(name = "expense", indexes = {
        @Index(name = "idx_expense_mess_date", columnList = "mess_id, date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mess_id", nullable = false)
    private Mess mess;

    /** The member (Manager) who made the purchase. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null)
            createdAt = Instant.now();
    }
}
