package com.bienCriollas.stock.Dto;

import java.time.LocalDate;
import java.util.Date;



public record StockDTO(
		
		 Long id_variedad,
			
			
		 LocalDate fecha_elaboracion,
		
		
		 Integer stock_total
) {}
