package com.bienCriollas.stock.variedad.dto;

import java.math.BigDecimal;

public record PrecioCostoVariedadDTO (
		 Long idVariedad,
	      BigDecimal precioUnitario
	     ) {}
