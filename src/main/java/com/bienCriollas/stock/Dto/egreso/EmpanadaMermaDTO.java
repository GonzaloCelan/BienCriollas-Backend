package com.bienCriollas.stock.Dto.egreso;

import java.math.BigDecimal;

public record EmpanadaMermaDTO(
		String nombre,
		Integer cantidad,
		BigDecimal montoPerdido 
		) {

}
