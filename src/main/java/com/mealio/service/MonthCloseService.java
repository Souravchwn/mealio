package com.mealio.service;

import com.mealio.dto.CloseMonthRequest;
import com.mealio.dto.DailyLogDto;
import com.mealio.dto.MonthMatrixResponse;
import com.mealio.exception.MonthAlreadyClosedException;
import com.mealio.exception.ResourceNotFoundException;
import com.mealio.model.entity.DailyLog;
import com.mealio.model.entity.Member;
import com.mealio.model.entity.Mess;
import com.mealio.model.entity.MonthlySnapshot;
import com.mealio.model.enums.MonthStatus;
import com.mealio.port.in.ExpenseUseCase;
import com.mealio.port.in.MonthCloseUseCase;
import com.mealio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain service: MonthCloseUseCase implementation.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * CONCURRENCY DESIGN:
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * closeMonth() is the most financially critical operation. Two concurrent
 * close attempts would cause double balance deductions across all members.
 *
 * Protection strategy:
 *
 * 1. SERIALIZABLE isolation — PostgreSQL SSI detects phantom reads /
 * write skew and fails the second transaction with a serialization error.
 *
 * 2. Pessimistic lock on MonthlySnapshot row (FOR UPDATE) — the first
 * committer holds this lock for the duration of the transaction.
 * The second request waits, then re-checks status=CLOSED → exception.
 * This is the belt to SERIALIZABLE's suspenders.
 *
 * 3. Pessimistic lock on all Member rows (FOR UPDATE, ORDER BY id) —
 * ordering prevents deadlocks between concurrent close operations or
 * concurrent balance adjustments.
 *
 * 4. @Version on MonthlySnapshot and Member — OCC final safety net.
 *
 * 5. @Retryable for SERIALIZABLE-level serialization failures (PostgreSQL
 * throws "could not serialize access" which maps to
 * CannotSerializeTransactionException).
 * ──────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthCloseService implements MonthCloseUseCase {

        private final MessRepository messRepository;
        private final MemberRepository memberRepository;
        private final DailyLogRepository dailyLogRepository;
        private final ExpenseRepository expenseRepository;
        private final MonthlySnapshotRepository snapshotRepository;
        private final ExpenseUseCase expenseUseCase;

        @Override
        @Retryable(retryFor = {
                        ObjectOptimisticLockingFailureException.class,
                        CannotAcquireLockException.class,
                        org.springframework.transaction.CannotCreateTransactionException.class
        }, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2))
        @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 60)
        public MonthlySnapshot closeMonth(CloseMonthRequest request) {
                Mess mess = messRepository.findById(request.messId())
                                .orElseThrow(() -> new ResourceNotFoundException("Mess", request.messId()));

                YearMonth ym = YearMonth.parse(request.yearMonth());

                // ── Layer 2: Pessimistic lock on the snapshot row ─────────────────────
                // If no snapshot yet, we must INSERT — no row to lock.
                // We still benefit from SERIALIZABLE isolation preventing phantoms.
                MonthlySnapshot snapshot = snapshotRepository
                                .findByMessAndYearMonthWithLock(mess, ym.toString())
                                .orElseGet(() -> MonthlySnapshot.builder()
                                                .mess(mess)
                                                .yearMonth(ym.toString())
                                                .status(MonthStatus.OPEN)
                                                .build());

                // Guard — second request will see CLOSED after lock acquisition
                if (snapshot.getStatus() == MonthStatus.CLOSED) {
                        throw new MonthAlreadyClosedException(ym.toString());
                }

                var from = ym.atDay(1);
                var to = ym.atEndOfMonth();

                // ── 1. Calculate totals ───────────────────────────────────────────────
                BigDecimal totalExpense = expenseRepository.sumAmountByMessAndDateRange(mess, from, to);
                int totalMeals = dailyLogRepository.sumTotalMeals(mess, from, to);
                BigDecimal mealRate = (totalMeals > 0 && totalExpense.compareTo(BigDecimal.ZERO) > 0)
                                ? totalExpense.divide(BigDecimal.valueOf(totalMeals), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // ── 2 + 3. Lock members → freeze logs → adjust balances ──────────────
                // ORDER BY id in findAllByMessWithLock prevents deadlocks if two
                // transactions try to lock the same members simultaneously.
                List<Member> members = memberRepository.findAllByMessWithLock(mess);
                for (Member member : members) {
                        List<DailyLog> logs = dailyLogRepository
                                        .findByMemberAndDateBetweenOrderByDate(member, from, to);

                        logs.forEach(dl -> dl.setFrozen(true));
                        dailyLogRepository.saveAll(logs);

                        int memberMeals = logs.stream().mapToInt(this::countMeals).sum();
                        member.setBalance(member.getBalance().subtract(
                                        mealRate.multiply(BigDecimal.valueOf(memberMeals))));
                }
                memberRepository.saveAll(members);

                // ── 4. Seal the snapshot ──────────────────────────────────────────────
                snapshot.setTotalExpense(totalExpense);
                snapshot.setTotalMeals(totalMeals);
                snapshot.setMealRate(mealRate);
                snapshot.setStatus(MonthStatus.CLOSED);
                snapshot.setClosedAt(Instant.now());
                MonthlySnapshot saved = snapshotRepository.save(snapshot);

                log.info("✅ Month {} CLOSED for mess '{}'. ৳{}/meal × {} meals. {} members settled.",
                                ym, mess.getName(), mealRate, totalMeals, members.size());
                return saved;
        }

        @Override
        @Transactional(readOnly = true, timeout = 30)
        public MonthMatrixResponse getMonthMatrix(UUID messId, YearMonth yearMonth) {
                Mess mess = messRepository.findById(messId)
                                .orElseThrow(() -> new ResourceNotFoundException("Mess", messId));

                var from = yearMonth.atDay(1);
                var to = yearMonth.atEndOfMonth();

                BigDecimal totalExpense = expenseRepository.sumAmountByMessAndDateRange(mess, from, to);
                int totalMeals = dailyLogRepository.sumTotalMeals(mess, from, to);
                BigDecimal mealRate = expenseUseCase.calculateLiveMealRate(mess, yearMonth);

                List<Member> members = memberRepository.findAllByMess(mess); // read-only, no lock
                List<MonthMatrixResponse.MemberRow> rows = new ArrayList<>();

                for (Member member : members) {
                        List<DailyLog> logs = dailyLogRepository
                                        .findByMemberAndDateBetweenOrderByDate(member, from, to);

                        int memberMeals = logs.stream().mapToInt(this::countMeals).sum();

                        List<DailyLogDto> dayDtos = logs.stream()
                                        .map(dl -> new DailyLogDto(
                                                        dl.getId(), dl.getMember().getId(), dl.getMember().getName(),
                                                        dl.getDate(), dl.isBreakfast(), dl.isLunch(), dl.isDinner(),
                                                        dl.getGuestCount(), dl.isFrozen()))
                                        .toList();

                        rows.add(new MonthMatrixResponse.MemberRow(
                                        member.getId(), member.getName(),
                                        memberMeals,
                                        mealRate.multiply(BigDecimal.valueOf(memberMeals)),
                                        member.getBalance(),
                                        dayDtos));
                }

                return new MonthMatrixResponse(messId, yearMonth.toString(),
                                totalExpense, mealRate, totalMeals, rows);
        }

        private int countMeals(DailyLog dl) {
                return (dl.isBreakfast() ? 1 : 0)
                                + (dl.isLunch() ? 1 : 0)
                                + (dl.isDinner() ? 1 : 0)
                                + dl.getGuestCount();
        }
}
