package com.bienCriollas.stock.stock.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producción de stock para una variedad y fecha.")
public record StockDTO(
		
		 @JsonProperty("id_variedad")
		 @Schema(description = "ID de la variedad.", example = "2") Long varietyId,
			
			
		 @JsonProperty("fecha_elaboracion")
		 @Schema(description = "Fecha de elaboración.", example = "2026-08-26") LocalDate productionDate,
		
		
		 @JsonProperty("stock_total")
		 @Schema(description = "Cantidad total elaborada.", example = "48") Integer totalStock
) {}
