package com.mealio.controller;

import com.mealio.dto.CloseMonthRequest;
import com.mealio.dto.MonthMatrixResponse;
import com.mealio.model.entity.MonthlySnapshot;
import com.mealio.port.in.MonthCloseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/**
 * FR-4: Admin Matrix and Month-Close.
 * Depends on MonthCloseUseCase interface.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

        private final MonthCloseUseCase monthCloseUseCase; // DIP

        @GetMapping("/matrix")
        public ResponseEntity<MonthMatrixResponse> getMatrix(
                        @RequestParam UUID messId,
                        @RequestParam(defaultValue = "") String yearMonth) {

                YearMonth ym = yearMonth.isBlank() ? YearMonth.now() : YearMonth.parse(yearMonth);
                return ResponseEntity.ok(monthCloseUseCase.getMonthMatrix(messId, ym));
        }

        @PostMapping("/close-month")
        public ResponseEntity<MonthCloseResponse> closeMonth(@Valid @RequestBody CloseMonthRequest request) {
                MonthlySnapshot snapshot = monthCloseUseCase.closeMonth(request);
                return ResponseEntity.ok(new MonthCloseResponse(
                                snapshot.getId(),
                                snapshot.getYearMonth(),
                                snapshot.getTotalExpense(),
                                snapshot.getMealRate(),
                                snapshot.getTotalMeals(),
                                snapshot.getStatus().name(),
                                snapshot.getClosedAt() != null ? snapshot.getClosedAt().toString() : null));
        }

        public record MonthCloseResponse(
                        UUID id,
                        String yearMonth,
                        BigDecimal totalExpense,
                        BigDecimal mealRate,
                        int totalMeals,
                        String status,
                        String closedAt) {
        }
}
