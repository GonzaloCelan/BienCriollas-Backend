package com.bienCriollas.stock.expense.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DailyExpensesDTO(
		@JsonProperty("totalEgresos") BigDecimal totalExpenses)
{}
