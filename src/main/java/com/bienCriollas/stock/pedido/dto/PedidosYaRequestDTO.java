package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PedidosYaRequestDTO(
		
		LocalDate fecha,
		BigDecimal monto) {

}
