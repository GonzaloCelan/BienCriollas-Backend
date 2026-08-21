package com.bienCriollas.stock.merma.dto;

import java.math.BigDecimal;

public record EmpanadaMermaDTO(
		String nombre,
		Integer cantidad,
		BigDecimal montoPerdido 
		) {

}
