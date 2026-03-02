package com.mealio.dto;

import com.mealio.model.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID messId,
        UUID memberId,
        String memberName,
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate date,
        Instant createdAt,
        BigDecimal liveMealRate // current meal rate after this entry
) {
}
