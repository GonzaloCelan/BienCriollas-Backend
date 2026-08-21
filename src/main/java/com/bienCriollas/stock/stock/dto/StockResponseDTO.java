package com.bienCriollas.stock.stock.dto;

import java.time.LocalDate;

public record StockResponseDTO (
		
			Long id_variedad,
			LocalDate fecha_elaboracion,
			Integer stock_total,
			Integer stock_disponible
		
		){}
