package com.mealio.model.entity;

import com.mealio.model.enums.MonthStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Immutable monthly snapshot created when admin "closes" a month.
 * This is the core Data Goldmine record — never updated after closure.
 */
@Entity
@Table(name = "monthly_snapshot", uniqueConstraints = @UniqueConstraint(columnNames = { "mess_id", "year_month" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Optimistic lock — prevents double month-close if two admin requests race. */
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mess_id", nullable = false)
    private Mess mess;

    /**
     * Stored as String "YYYY-MM" — JPA does not natively support YearMonth.
     * Access via getYearMonth() / setYearMonth().
     */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "total_expense", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "total_meals", nullable = false)
    @Builder.Default
    private int totalMeals = 0;

    /** Meal rate = totalExpense / totalMeals (rounded to 2 dp). */
    @Column(name = "meal_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal mealRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MonthStatus status = MonthStatus.OPEN;

    @Column(name = "closed_at")
    private Instant closedAt;

    // ── convenience methods ────────────────────────────────────────────────────

    public YearMonth getYearMonthValue() {
        return YearMonth.parse(yearMonth);
    }

    public void setYearMonthValue(YearMonth ym) {
        this.yearMonth = ym.toString();
    }
}
