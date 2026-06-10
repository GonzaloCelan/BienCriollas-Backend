package com.bienCriollas.stock.Dto.egreso;

import java.math.BigDecimal;

public record EgresoResponseDTO(
		
		BigDecimal totalPersonal,
		BigDecimal totalProduccion,
		BigDecimal totalOtros
		
		) {

}
