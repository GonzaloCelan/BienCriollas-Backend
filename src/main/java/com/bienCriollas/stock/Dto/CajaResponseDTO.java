package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record CajaResponseDTO(
		
		BigDecimal ingresosTotales,
        BigDecimal ingresosEfectivo,
        BigDecimal ingresosTransferencias,
        BigDecimal totalEgresos,
        BigDecimal pedidosYaLiquidacion
        ) {

}
