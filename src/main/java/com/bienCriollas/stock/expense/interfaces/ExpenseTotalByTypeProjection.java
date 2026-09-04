package com.bienCriollas.stock.expense.interfaces;

import java.math.BigDecimal;

import com.bienCriollas.stock.expense.enums.ExpenseType;

public interface ExpenseTotalByTypeProjection {

    ExpenseType getExpenseType();

    BigDecimal getTotal();
}
