package com.bienCriollas.stock.waste.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unidades perdidas o descartadas de una variedad.")
public record EmpanadaLossDTO(
        @JsonProperty("idVariedad")
        @Schema(description = "ID de la variedad.", example = "2") Long varietyId,
        @JsonProperty("cantidad")
        @Schema(description = "Cantidad perdida.", example = "3") Integer quantity
        
) {}
