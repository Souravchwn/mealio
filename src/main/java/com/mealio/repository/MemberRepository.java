package com.mealio.repository;

import com.mealio.model.entity.Member;
import com.mealio.model.entity.Mess;
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
public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByPhone(String phone);

    Optional<Member> findByTelegramUserId(Long telegramUserId);

    List<Member> findAllByMess(Mess mess);

    boolean existsByPhone(String phone);

    boolean existsByTelegramUserId(Long telegramUserId);

    // ── Pessimistic Locking ───────────────────────────────────────────────────

    /**
     * SELECT ... FOR UPDATE on the member row.
     *
     * Used in MealToggleService to serialize all DailyLog operations for
     * the same member. Different members can proceed fully in parallel.
     *
     * 5-second lock timeout prevents indefinite blocking if another
     * transaction holds the lock for too long (e.g. due to a crash).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdWithLock(@Param("id") UUID id);

    /**
     * SELECT ... FOR UPDATE on ALL member rows belonging to a mess.
     *
     * Used in MonthCloseService to serialize balance adjustments across
     * all members during the atomic month-close operation.
     *
     * Row order is deterministic (by id) to prevent deadlocks between
     * concurrent month-close calls on the same mess.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000"))
    @Query("SELECT m FROM Member m WHERE m.mess = :mess ORDER BY m.id")
    List<Member> findAllByMessWithLock(@Param("mess") Mess mess);
}
