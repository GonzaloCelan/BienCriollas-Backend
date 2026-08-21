package com.bienCriollas.stock.stock.dto;

import java.time.LocalDate;



public record StockDTO(
		
		 Long id_variedad,
			
			
		 LocalDate fecha_elaboracion,
		
		
		 Integer stock_total
) {}
