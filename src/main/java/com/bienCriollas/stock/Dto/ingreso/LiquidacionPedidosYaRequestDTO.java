package com.bienCriollas.stock.Dto.ingreso;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidacionPedidosYaRequestDTO(
        LocalDate fecha,
        BigDecimal monto,
        String descripcion
) {
}