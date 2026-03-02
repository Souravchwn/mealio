package com.mealio.repository;

import com.mealio.model.entity.Mess;
import com.mealio.model.entity.MonthlySnapshot;
import com.mealio.model.enums.MonthStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlySnapshotRepository extends JpaRepository<MonthlySnapshot, UUID> {

    Optional<MonthlySnapshot> findByMessAndYearMonth(Mess mess, String yearMonth);

    Optional<MonthlySnapshot> findByMessAndStatus(Mess mess, MonthStatus status);

    List<MonthlySnapshot> findByMessOrderByYearMonthDesc(Mess mess);

    // ── Pessimistic Locking ───────────────────────────────────────────────────

    /**
     * SELECT ... FOR UPDATE on the MonthlySnapshot row.
     *
     * CRITICAL for preventing double month-close:
     * - Admin A and Admin B both click "Close Month" simultaneously.
     * - Both hit this query. Only ONE gets the lock immediately.
     * - The other waits. When it acquires the lock, it re-reads the row,
     * sees status=CLOSED, and MonthAlreadyClosedException is thrown.
     * - No double balance deduction. No data corruption.
     *
     * Without this lock, both could read status=OPEN and proceed concurrently.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000"))
    @Query("SELECT s FROM MonthlySnapshot s WHERE s.mess = :mess AND s.yearMonth = :yearMonth")
    Optional<MonthlySnapshot> findByMessAndYearMonthWithLock(
            @Param("mess") Mess mess,
            @Param("yearMonth") String yearMonth);
}
