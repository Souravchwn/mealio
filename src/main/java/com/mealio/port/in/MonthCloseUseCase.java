package com.mealio.port.in;

import com.mealio.dto.CloseMonthRequest;
import com.mealio.dto.MonthMatrixResponse;
import com.mealio.model.entity.MonthlySnapshot;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Use-case interface: admin month-close and matrix view.
 */
public interface MonthCloseUseCase {

    MonthlySnapshot closeMonth(CloseMonthRequest request);

    MonthMatrixResponse getMonthMatrix(UUID messId, YearMonth yearMonth);
}
