package com.bienCriollas.stock.variety.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VarietyRequestDTO(

        @JsonProperty("nombre") String name,
        @JsonProperty("precio_unitario") BigDecimal unitPrice
        ) {

}
