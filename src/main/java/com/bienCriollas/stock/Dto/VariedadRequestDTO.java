package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record VariedadRequestDTO(
		
		String nombre,
		BigDecimal precio_unitario
		) {

}
