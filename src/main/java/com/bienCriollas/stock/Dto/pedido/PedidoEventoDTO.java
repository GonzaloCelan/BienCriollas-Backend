package com.bienCriollas.stock.Dto.pedido;

public record PedidoEventoDTO(
        String tipo,
        Long idPedido,
        String estado
) {}