package com.bienCriollas.stock.stock.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producción de stock para una variedad y fecha.")
public record StockDTO(
		
		 @Schema(description = "ID de la variedad.", example = "2") Long id_variedad,
			
			
		 @Schema(description = "Fecha de elaboración.", example = "2026-08-26") LocalDate fecha_elaboracion,
		
		
		 @Schema(description = "Cantidad total elaborada.", example = "48") Integer stock_total
) {}
