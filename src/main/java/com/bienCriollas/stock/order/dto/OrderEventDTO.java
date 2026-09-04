package com.bienCriollas.stock.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderEventDTO(
        @JsonProperty("tipo") String type,
        @JsonProperty("idPedido") Long orderId,
        @JsonProperty("estado") String status
) {}
