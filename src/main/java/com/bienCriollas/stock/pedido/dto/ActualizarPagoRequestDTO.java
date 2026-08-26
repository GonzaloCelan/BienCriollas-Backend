package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Importes requeridos cuando el nuevo medio de pago es COMBINADO.")
public record ActualizarPagoRequestDTO(
        @Schema(description = "Parte abonada en efectivo.", example = "5000.00")
        BigDecimal montoEfectivo,
        @Schema(description = "Parte abonada por transferencia.", example = "4000.00")
        BigDecimal montoTransferencia) {
}
