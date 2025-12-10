package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record PedidoDetalleRequestDTO(
        Long idVariedad,      // id de la variedad de empanada
        Integer cantidad
) {}
