package com.bienCriollas.stock.expense.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ExpenseTotalByTypeDTO(
        @JsonProperty("tipoEgreso") ExpenseType expenseType,
        @JsonProperty("total") BigDecimal total
) {
}
