package com.bienCriollas.stock.pedido.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Una variedad y su cantidad dentro del pedido.")
public record PedidoDetalleRequestDTO(
        @Schema(description = "ID de la variedad de empanada.", example = "2") Long idVariedad,
        @Schema(description = "Cantidad solicitada.", example = "6") Integer cantidad
) {}
