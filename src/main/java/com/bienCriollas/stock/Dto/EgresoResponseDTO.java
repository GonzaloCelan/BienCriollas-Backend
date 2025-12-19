package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record EgresoResponseDTO(
		
		BigDecimal totalPersonal,
		BigDecimal totalProduccion,
		BigDecimal totalOtros
		
		) {

}
