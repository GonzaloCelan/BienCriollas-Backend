package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.enums.TipoPago;
import com.bienCriollas.stock.pedido.enums.TipoVenta;

public record RegularizacionPedidoItemDTO(
        Long idPedido,
        LocalDate fechaPedido,
        String cliente,
        TipoVenta tipoVenta,
        TipoPago tipoPago,
        String numeroPedidoPedidosYa,
        LocalTime horaEntrega,
        BigDecimal totalPedido,
        TipoEstado estadoActual) {
}
