package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PedidosYaRequestDTO(
		
		LocalDate fecha,
		BigDecimal monto) {

}
