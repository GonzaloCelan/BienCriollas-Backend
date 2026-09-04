package com.bienCriollas.stock.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ExpenseRequestDTO(
		@JsonProperty("idCaja") Long cashRegisterId,
		@JsonProperty("tipoEgreso") ExpenseType expenseType,
		@JsonProperty("fecha") LocalDate date,
        @JsonProperty("descripcion") String description,
        @JsonProperty("monto") BigDecimal amount
) {}
