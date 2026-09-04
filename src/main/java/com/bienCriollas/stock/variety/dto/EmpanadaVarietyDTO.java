package com.bienCriollas.stock.variety.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmpanadaVarietyDTO(
		@JsonProperty("idVariedad") Long varietyId,
	    @JsonProperty("precioUnitario") BigDecimal unitPrice) {

}
