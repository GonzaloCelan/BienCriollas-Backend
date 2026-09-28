package com.bienCriollas.stock.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateDeliveryEtaRequestDTO(
        @JsonProperty("minutos")
        @Schema(
                description = "Minutos restantes informados por PedidosYa. Enviar null para quitar el ETA.",
                example = "31",
                nullable = true)
        Integer minutes
) {}
