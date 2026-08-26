package com.bienCriollas.stock.ingreso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liquidación recibida desde PedidosYa.")
public record LiquidacionPedidosYaRequestDTO(
        @Schema(description = "Fecha de la liquidación.", example = "2026-08-26") LocalDate fecha,
        @Schema(description = "Importe liquidado.", example = "150000.00") BigDecimal monto,
        @Schema(description = "Referencia opcional.", example = "Liquidación semanal") String descripcion
) {
}
