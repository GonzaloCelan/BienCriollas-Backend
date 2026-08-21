package com.bienCriollas.stock.variedad.dto;

import java.math.BigDecimal;

public record VariedadRequestDTO(
		
		String nombre,
		BigDecimal precio_unitario
		) {

}
