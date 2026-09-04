package com.bienCriollas.stock.expense.interfaces;

import java.math.BigDecimal;

public interface MonthlyExpenseTotalsProjection {

         String getExpenseType();
        BigDecimal getCurrentMonthTotal();
        BigDecimal getPreviousMonthTotal();
}
