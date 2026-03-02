package com.mealio.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full-month admin matrix: all members x all dates.
 */
public record MonthMatrixResponse(
        UUID messId,
        String yearMonth,
        BigDecimal totalExpense,
        BigDecimal mealRate,
        int totalMeals,
        List<MemberRow> members) {
    public record MemberRow(
            UUID memberId,
            String memberName,
            int totalMeals,
            BigDecimal totalCost,
            BigDecimal balance,
            List<DailyLogDto> days) {
    }
}
