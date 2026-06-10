package com.bienCriollas.stock.Dto.pedido;

public record PedidoDetalleRequestDTO(
        Long idVariedad,      // id de la variedad de empanada
        Integer cantidad
) {}
