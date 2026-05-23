package com.bienCriollas.stock.Dto;

public record PedidoEventoDTO(
        String tipo,
        Long idPedido,
        String estado
) {}