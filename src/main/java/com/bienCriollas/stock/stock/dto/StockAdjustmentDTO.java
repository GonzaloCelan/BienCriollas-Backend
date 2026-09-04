package com.bienCriollas.stock.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corrección manual del stock disponible.")
public record StockAdjustmentDTO(
        @JsonProperty("idVariedad")
        @Schema(description = "ID de la variedad.", example = "2") Long varietyId,
        @JsonProperty("stockDisponible")
        @Schema(description = "Nueva cantidad disponible.", example = "24") Integer availableStock
) {}
