package com.bienCriollas.stock.pedido.dto;

public record PedidoEventoDTO(
        String tipo,
        Long idPedido,
        String estado
) {}