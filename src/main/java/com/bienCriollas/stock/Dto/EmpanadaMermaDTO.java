package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record EmpanadaMermaDTO(
		String nombre,
		Integer cantidad,
		BigDecimal montoPerdido 
		) {

}
