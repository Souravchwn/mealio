package com.mealio.port.in;

import com.mealio.dto.ExpenseRequest;
import com.mealio.dto.ExpenseResponse;
import com.mealio.model.entity.Mess;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Use-case interface: bazaar expense management.
 */
public interface ExpenseUseCase {

    ExpenseResponse addExpense(ExpenseRequest request);

    List<ExpenseResponse> getExpenses(UUID messId, YearMonth yearMonth);

    BigDecimal calculateLiveMealRate(Mess mess, YearMonth yearMonth);
}
