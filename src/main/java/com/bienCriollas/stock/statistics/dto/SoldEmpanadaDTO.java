package com.bienCriollas.stock.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SoldEmpanadaDTO(

        @JsonProperty("nombre") String name,
        @JsonProperty("cantidad") Integer quantity
        ) {

}
