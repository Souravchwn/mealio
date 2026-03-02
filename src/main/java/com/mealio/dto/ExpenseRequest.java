package com.mealio.dto;

import com.mealio.model.enums.ExpenseCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(

        @NotNull(message = "messId is required") UUID messId,

        @NotNull(message = "memberId (manager) is required") UUID memberId,

        @NotNull(message = "amount is required") @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,

        @NotNull(message = "category is required") ExpenseCategory category,

        @Size(max = 255) String description,

        @NotNull(message = "date is required") LocalDate date) {
}
