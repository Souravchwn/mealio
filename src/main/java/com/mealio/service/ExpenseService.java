package com.mealio.service;

import com.mealio.dto.ExpenseRequest;
import com.mealio.dto.ExpenseResponse;
import com.mealio.exception.ResourceNotFoundException;
import com.mealio.model.entity.Expense;
import com.mealio.model.entity.Member;
import com.mealio.model.entity.Mess;
import com.mealio.port.in.ExpenseUseCase;
import com.mealio.repository.DailyLogRepository;
import com.mealio.repository.ExpenseRepository;
import com.mealio.repository.MemberRepository;
import com.mealio.repository.MessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Domain service: implements ExpenseUseCase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService implements ExpenseUseCase {

        private final ExpenseRepository expenseRepository;
        private final MessRepository messRepository;
        private final MemberRepository memberRepository;
        private final DailyLogRepository dailyLogRepository;

        @Override
        @Transactional
        public ExpenseResponse addExpense(ExpenseRequest request) {
                Mess mess = messRepository.findById(request.messId())
                                .orElseThrow(() -> new ResourceNotFoundException("Mess", request.messId()));

                Member manager = memberRepository.findById(request.memberId())
                                .orElseThrow(() -> new ResourceNotFoundException("Member", request.memberId()));

                Expense expense = Expense.builder()
                                .mess(mess)
                                .member(manager)
                                .amount(request.amount())
                                .category(request.category())
                                .description(request.description())
                                .date(request.date())
                                .build();

                expense = expenseRepository.save(expense);
                BigDecimal mealRate = calculateLiveMealRate(mess, YearMonth.from(request.date()));

                log.info("Expense {} ({}) ৳{} saved for mess '{}'. Live rate: ৳{}/meal",
                                expense.getId(), expense.getCategory(), expense.getAmount(), mess.getName(), mealRate);

                return toResponse(expense, mealRate);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ExpenseResponse> getExpenses(UUID messId, YearMonth yearMonth) {
                Mess mess = messRepository.findById(messId)
                                .orElseThrow(() -> new ResourceNotFoundException("Mess", messId));

                BigDecimal mealRate = calculateLiveMealRate(mess, yearMonth);
                var from = yearMonth.atDay(1);
                var to = yearMonth.atEndOfMonth();

                return expenseRepository.findByMessAndDateBetweenOrderByDateDesc(mess, from, to)
                                .stream()
                                .map(e -> toResponse(e, mealRate))
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public BigDecimal calculateLiveMealRate(Mess mess, YearMonth yearMonth) {
                var from = yearMonth.atDay(1);
                var to = yearMonth.atEndOfMonth();

                BigDecimal totalExpense = expenseRepository.sumAmountByMessAndDateRange(mess, from, to);
                int totalMeals = dailyLogRepository.sumTotalMeals(mess, from, to);

                if (totalMeals == 0 || totalExpense.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                }
                return totalExpense.divide(BigDecimal.valueOf(totalMeals), 2, RoundingMode.HALF_UP);
        }

        private ExpenseResponse toResponse(Expense e, BigDecimal mealRate) {
                return new ExpenseResponse(
                                e.getId(), e.getMess().getId(),
                                e.getMember().getId(), e.getMember().getName(),
                                e.getAmount(), e.getCategory(),
                                e.getDescription(), e.getDate(),
                                e.getCreatedAt(), mealRate);
        }
}
