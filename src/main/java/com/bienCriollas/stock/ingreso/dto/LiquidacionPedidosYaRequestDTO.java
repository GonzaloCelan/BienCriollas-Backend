package com.bienCriollas.stock.ingreso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidacionPedidosYaRequestDTO(
        LocalDate fecha,
        BigDecimal monto,
        String descripcion
) {
}