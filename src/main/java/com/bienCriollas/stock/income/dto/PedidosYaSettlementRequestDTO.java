package com.bienCriollas.stock.income.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liquidación recibida desde PedidosYa.")
public record PedidosYaSettlementRequestDTO(
        @JsonProperty("fecha") @Schema(description = "Fecha de la liquidación.", example = "2026-08-26") LocalDate date,
        @JsonProperty("monto") @Schema(description = "Importe liquidado.", example = "150000.00") BigDecimal amount,
        @JsonProperty("descripcion") @Schema(description = "Referencia opcional.", example = "Liquidación semanal") String description
) {
}
