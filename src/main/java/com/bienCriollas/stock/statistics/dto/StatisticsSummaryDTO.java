package com.bienCriollas.stock.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatisticsSummaryDTO(
        @JsonProperty("pedidosEntregados") Integer deliveredOrders,
        @JsonProperty("empanadasVendidas") Integer soldEmpanadas,
        @JsonProperty("ticketPromedio") BigDecimal averageTicket,
        @JsonProperty("variedadMasVendida") BestSellingVarietyDTO bestSellingVariety,

        @JsonProperty("rankingVariedades") List<VarietyRankingDTO> varietyRanking,
        @JsonProperty("ventasPorDiaSemana") List<SalesByWeekdayDTO> salesByWeekday,
        @JsonProperty("tiposVenta") List<SaleTypeSummaryDTO> saleTypes,
        @JsonProperty("mediosPago") List<PaymentMethodSummaryDTO> paymentMethods,
        @JsonProperty("mermasPorVariedad") List<WasteByVarietyDTO> wasteByVariety
) {

    public record BestSellingVarietyDTO(
            @JsonProperty("idVariedad") Long varietyId,
            @JsonProperty("nombre") String name,
            @JsonProperty("unidadesVendidas") Integer unitsSold
    ) {
    }

    public record VarietyRankingDTO(
            @JsonProperty("idVariedad") Long varietyId,
            @JsonProperty("nombre") String name,
            @JsonProperty("unidadesVendidas") Integer unitsSold
    ) {
    }

    public record SalesByWeekdayDTO(
            @JsonProperty("diaSemana") Integer weekday,
            @JsonProperty("nombreDia") String weekdayName,
            @JsonProperty("cantidadPedidos") Integer orderCount,
            @JsonProperty("unidadesVendidas") Integer unitsSold,
            @JsonProperty("totalVendido") BigDecimal totalSales
    ) {
    }

    public record SaleTypeSummaryDTO(
            @JsonProperty("tipoVenta") String saleType,
            @JsonProperty("cantidadPedidos") Integer orderCount,
            @JsonProperty("porcentaje") BigDecimal percentage
    ) {
    }

    public record PaymentMethodSummaryDTO(
            @JsonProperty("medioPago") String paymentMethod,
            @JsonProperty("cantidadPedidos") Integer orderCount,
            @JsonProperty("porcentaje") BigDecimal percentage
    ) {
    }

    public record WasteByVarietyDTO(
            @JsonProperty("idVariedad") Long varietyId,
            @JsonProperty("nombre") String name,
            @JsonProperty("unidadesPerdidas") Integer unitsLost
    ) {
    }
}
