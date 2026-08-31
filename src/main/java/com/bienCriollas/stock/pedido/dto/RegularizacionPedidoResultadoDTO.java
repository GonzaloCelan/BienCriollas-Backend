package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RegularizacionPedidoResultadoDTO(
        String idLote,
        int anio,
        int mes,
        int cantidadActualizada,
        BigDecimal ingresoIncorporado,
        List<Long> idsPedidos,
        String realizadoPor,
        OffsetDateTime realizadoEn) {
}
