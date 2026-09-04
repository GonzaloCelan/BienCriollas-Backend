package com.bienCriollas.stock.expense.repository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.expense.interfaces.ExpenseTotalByTypeProjection;
import com.bienCriollas.stock.expense.entity.Expense;
import com.bienCriollas.stock.expense.enums.ExpenseType;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /*
     * LISTADOS POR RANGO
     * Sirve para día, mes, semana, etc.
     */
    List<Expense> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    Page<Expense> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /*
     * LISTADO POR TIPO
     */
    Page<Expense> findByExpenseTypeOrderByCreatedAtDesc(
            ExpenseType expenseType,
            Pageable pageable
    );

    /*
     * LISTADO POR MES + TIPO
     */
    Page<Expense> findByExpenseTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            ExpenseType expenseType,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /*
     * ÚLTIMOS MOVIMIENTOS
     */
    List<Expense> findTop5ByOrderByCreatedAtDesc();

    /*
     * TOTAL GENERAL ENTRE FECHAS
     */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.createdAt >= :start
          AND e.createdAt < :end
    """)
    BigDecimal sumTotalBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /*
     * TOTAL POR TIPO ENTRE FECHAS
     */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.expenseType = :expenseType
          AND e.createdAt >= :start
          AND e.createdAt < :end
    """)
    BigDecimal sumTotalByTypeBetweenDates(
            @Param("expenseType") ExpenseType expenseType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /*
     * TOTALES AGRUPADOS POR TIPO
     */
    @Query("""
        SELECT
            e.expenseType AS expenseType,
            COALESCE(SUM(e.amount), 0) AS total
        FROM Expense e
        WHERE e.createdAt >= :start
          AND e.createdAt < :end
        GROUP BY e.expenseType
    """)
    List<ExpenseTotalByTypeProjection> getTotalsByTypeBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


}
