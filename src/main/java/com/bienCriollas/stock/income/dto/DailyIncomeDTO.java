package com.bienCriollas.stock.income.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DailyIncomeDTO(
		@JsonProperty("ingresosEfectivo") BigDecimal cashIncome,
		@JsonProperty("ingresosTransferencia") BigDecimal transferIncome,
		@JsonProperty("ingresosTotal") BigDecimal totalIncome)
{
}
