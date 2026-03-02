package com.mealio.repository;

import com.mealio.model.entity.DailyLog;
import com.mealio.model.entity.Member;
import com.mealio.model.entity.Mess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, UUID> {

    Optional<DailyLog> findByMemberAndDate(Member member, LocalDate date);

    /** All logs for a mess on a given date (for headcount calculation). */
    @Query("SELECT dl FROM DailyLog dl JOIN dl.member m WHERE m.mess = :mess AND dl.date = :date AND dl.frozen = false")
    List<DailyLog> findByMessAndDate(@Param("mess") Mess mess, @Param("date") LocalDate date);

    /** All logs for a member within a date range (for admin matrix). */
    List<DailyLog> findByMemberAndDateBetweenOrderByDate(Member member, LocalDate from, LocalDate to);

    /**
     * Total meals eaten by a member in a date range.
     * Each B/L/D = 1 meal; each guest meal = 1 additional meal.
     */
    @Query("""
            SELECT COALESCE(SUM(
                (CASE WHEN dl.breakfast THEN 1 ELSE 0 END) +
                (CASE WHEN dl.lunch     THEN 1 ELSE 0 END) +
                (CASE WHEN dl.dinner    THEN 1 ELSE 0 END) +
                dl.guestCount
            ), 0)
            FROM DailyLog dl
            WHERE dl.member.mess = :mess
              AND dl.date BETWEEN :from AND :to
            """)
    int sumTotalMeals(@Param("mess") Mess mess, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
