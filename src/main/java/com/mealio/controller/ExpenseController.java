package com.mealio.controller;

import com.mealio.dto.ExpenseRequest;
import com.mealio.dto.ExpenseResponse;
import com.mealio.port.in.ExpenseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * FR-2: Bazaar expense management REST API.
 * Depends on ExpenseUseCase interface — not on ExpenseService directly.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseUseCase expenseUseCase; // DIP

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseUseCase.addExpense(request));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam UUID messId,
            @RequestParam(defaultValue = "") String yearMonth) {

        YearMonth ym = yearMonth.isBlank() ? YearMonth.now() : YearMonth.parse(yearMonth);
        return ResponseEntity.ok(expenseUseCase.getExpenses(messId, ym));
    }

    @GetMapping("/meal-rate")
    public ResponseEntity<MealRateResponse> getMealRate(
            @RequestParam UUID messId,
            @RequestParam(defaultValue = "") String yearMonth) {

        YearMonth ym = yearMonth.isBlank() ? YearMonth.now() : YearMonth.parse(yearMonth);

        // Delegate total rate retrieval via the use case
        List<ExpenseResponse> expenses = expenseUseCase.getExpenses(messId, ym);
        BigDecimal rate = expenses.stream()
                .findFirst()
                .map(ExpenseResponse::liveMealRate)
                .orElse(BigDecimal.ZERO);

        return ResponseEntity.ok(new MealRateResponse(messId, ym.toString(), rate));
    }

    public record MealRateResponse(UUID messId, String yearMonth, BigDecimal mealRate) {
    }
}
