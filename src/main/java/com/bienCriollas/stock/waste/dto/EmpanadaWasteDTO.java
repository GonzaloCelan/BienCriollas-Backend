package com.bienCriollas.stock.waste.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmpanadaWasteDTO(
		@JsonProperty("nombre") String name,
		@JsonProperty("cantidad") Integer quantity,
		@JsonProperty("montoPerdido") BigDecimal lostAmount
		) {

}
