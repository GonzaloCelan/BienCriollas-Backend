package com.bienCriollas.stock.egreso.dto;

import java.math.BigDecimal;

public record EgresoResponseDTO(
		
		BigDecimal totalPersonal,
		BigDecimal totalProduccion,
		BigDecimal totalOtros
		
		) {

}
