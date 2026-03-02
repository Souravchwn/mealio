package com.mealio.repository;

import com.mealio.model.entity.Expense;
import com.mealio.model.entity.Mess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByMessAndDateBetweenOrderByDateDesc(Mess mess, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.mess = :mess AND e.date BETWEEN :from AND :to")
    BigDecimal sumAmountByMessAndDateRange(@Param("mess") Mess mess,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
