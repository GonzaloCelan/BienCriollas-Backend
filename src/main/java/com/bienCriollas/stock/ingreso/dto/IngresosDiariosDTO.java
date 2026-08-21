package com.bienCriollas.stock.ingreso.dto;

import java.math.BigDecimal;

public record IngresosDiariosDTO(
		BigDecimal ingresosEfectivo,
		BigDecimal ingresosTransferencia,
		BigDecimal ingresosTotal) 
{
}
