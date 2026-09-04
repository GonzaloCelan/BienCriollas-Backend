package com.bienCriollas.stock.expense.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.expense.dto.ExpenseResponseDTO;
import com.bienCriollas.stock.expense.dto.ExpenseTypeDTO;
import com.bienCriollas.stock.expense.dto.ExpenseTotalByTypeDTO;
import com.bienCriollas.stock.expense.dto.ExpensePercentageDTO;
import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.bienCriollas.stock.expense.entity.Expense;

public interface IExpenseService {

    Expense registerExpense(ExpenseTypeDTO request);

    ExpenseResponseDTO calculateAccumulatedExpenses();

    List<ExpensePercentageDTO> getCurrentVsPreviousMonthMetrics();

    List<ExpenseTotalByTypeDTO> getTotalsByType(int year, int month);

    List<Expense> getTodaysExpenses();

    Page<Expense> getHistory(
            int year,
            int month,
            ExpenseType expenseType,
            Pageable pageable
    );

    Page<Expense> getHistory(
            int year,
            int month,
            Pageable pageable
    );

    Page<Expense> getByExpenseType(
            ExpenseType expenseType,
            Pageable pageable
    );

    List<Expense> getLatestMovements();
}
