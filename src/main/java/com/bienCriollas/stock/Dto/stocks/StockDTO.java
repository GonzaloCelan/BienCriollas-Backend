package com.bienCriollas.stock.Dto.stocks;

import java.time.LocalDate;



public record StockDTO(
		
		 Long id_variedad,
			
			
		 LocalDate fecha_elaboracion,
		
		
		 Integer stock_total
) {}
