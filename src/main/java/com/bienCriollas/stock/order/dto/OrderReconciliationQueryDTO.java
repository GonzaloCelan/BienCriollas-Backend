package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderReconciliationQueryDTO(
        @JsonProperty("anio") int year,
        @JsonProperty("mes") int month,
        @JsonProperty("cantidadPedidos") long orderCount,
        @JsonProperty("ingresoPotencial") BigDecimal potentialIncome,
        @JsonProperty("pedidos") List<OrderReconciliationItemDTO> orders,
        @JsonProperty("pagina") int page,
        @JsonProperty("tamanoPagina") int pageSize,
        @JsonProperty("totalPaginas") int totalPages) {
}
