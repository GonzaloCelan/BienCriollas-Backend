package com.bienCriollas.stock.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Una variedad y su cantidad dentro del pedido.")
public record OrderDetailRequestDTO(
        @JsonProperty("idVariedad")
        @Schema(description = "ID de la variedad de empanada.", example = "2") Long varietyId,
        @JsonProperty("cantidad")
        @Schema(description = "Cantidad solicitada.", example = "6") Integer quantity
) {}
