package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record PrecioCostoVariedadDTO (
		 Long idVariedad,
	      BigDecimal precioUnitario
	     ) {}
