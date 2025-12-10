package com.bienCriollas.stock.Dto;

import java.time.LocalDate;
import java.util.Date;

public record StockResponseDTO (
		
			Long id_variedad,
			LocalDate fecha_elaboracion,
			Integer stock_total,
			Integer stock_disponible
		
		){}
