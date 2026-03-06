package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record IngresosDiariosDTO(
		BigDecimal ingresosEfectivo,
		BigDecimal ingresosTransferencia,
		BigDecimal ingresosTotal) 
{
}
