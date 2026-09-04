
package com.bienCriollas.stock.expense.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ExpensePercentageDTO(
		@JsonProperty("tipoEgreso") ExpenseType expenseType,
        @JsonProperty("totalMesActual") BigDecimal currentMonthTotal,
        @JsonProperty("porcentaje") BigDecimal percentage) {

}
