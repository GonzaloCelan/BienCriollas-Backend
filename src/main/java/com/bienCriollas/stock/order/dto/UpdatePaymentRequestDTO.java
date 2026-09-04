package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Importes requeridos cuando el nuevo medio de pago es COMBINADO.")
public record UpdatePaymentRequestDTO(
        @JsonProperty("montoEfectivo")
        @Schema(description = "Parte abonada en efectivo.", example = "5000.00")
        BigDecimal cashAmount,
        @JsonProperty("montoTransferencia")
        @Schema(description = "Parte abonada por transferencia.", example = "4000.00")
        BigDecimal transferAmount) {
}
