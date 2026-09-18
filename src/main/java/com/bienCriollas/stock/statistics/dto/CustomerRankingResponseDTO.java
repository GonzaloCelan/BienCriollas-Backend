package com.bienCriollas.stock.statistics.dto;

import java.math.BigDecimal;
import java.util.List;
import com.bienCriollas.stock.statistics.enums.CustomerRankingOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerRankingResponseDTO(
        @JsonProperty("periodo") PeriodDTO period,
        @JsonProperty("orden") CustomerRankingOrder order,
        @JsonProperty("totalClientes") int totalCustomers,
        @JsonProperty("ventasParticularesPeriodo") BigDecimal particularSales,
        @JsonProperty("totalTopClientes") BigDecimal topSales,
        @JsonProperty("porcentajeVentasTop") BigDecimal topPercentage,
        @JsonProperty("clientes") List<CustomerRankingItemDTO> customers
) {
    public record CustomerRankingItemDTO(
            @JsonProperty("posicion") int position,
            @JsonProperty("cliente") String customer,
            @JsonProperty("cantidadPedidos") long orders,
            @JsonProperty("totalAcumulado") BigDecimal totalSales,
            @JsonProperty("ticketPromedio") BigDecimal averageTicket,
            @JsonProperty("totalUnidades") long totalUnits
    ) {}
}
