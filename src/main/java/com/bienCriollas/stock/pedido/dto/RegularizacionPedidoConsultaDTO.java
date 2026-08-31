package com.bienCriollas.stock.pedido.dto;

import java.math.BigDecimal;
import java.util.List;

public record RegularizacionPedidoConsultaDTO(
        int anio,
        int mes,
        long cantidadPedidos,
        BigDecimal ingresoPotencial,
        List<RegularizacionPedidoItemDTO> pedidos,
        int pagina,
        int tamanoPagina,
        int totalPaginas) {
}
