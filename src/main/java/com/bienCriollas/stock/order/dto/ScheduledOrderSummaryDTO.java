package com.bienCriollas.stock.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScheduledOrderSummaryDTO(
        @JsonProperty("totalPedidosProgramados") long totalScheduledOrders,
        @JsonProperty("pedidosParaHoy") long ordersForToday,
        @JsonProperty("pedidosParaManana") long ordersForTomorrow,
        @JsonProperty("totalUnidadesComprometidas") long totalCommittedUnits) {
}
