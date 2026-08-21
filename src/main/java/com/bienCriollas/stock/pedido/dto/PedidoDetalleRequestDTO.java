package com.bienCriollas.stock.pedido.dto;

public record PedidoDetalleRequestDTO(
        Long idVariedad,      // id de la variedad de empanada
        Integer cantidad
) {}
