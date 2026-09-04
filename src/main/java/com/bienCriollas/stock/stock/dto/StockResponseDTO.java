package com.bienCriollas.stock.stock.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StockResponseDTO (
		
			@JsonProperty("id_variedad") Long varietyId,
			@JsonProperty("fecha_elaboracion") LocalDate productionDate,
			@JsonProperty("stock_total") Integer totalStock,
			@JsonProperty("stock_disponible") Integer availableStock
		
		){}
