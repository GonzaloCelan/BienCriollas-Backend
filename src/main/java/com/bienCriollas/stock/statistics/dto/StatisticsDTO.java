package com.bienCriollas.stock.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

import com.bienCriollas.stock.waste.dto.EmpanadaWasteDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StatisticsDTO(

        @JsonProperty("totalEmpanadasVendidas") Integer totalEmpanadasSold,
        @JsonProperty("totalMermas") Integer totalWaste,
        @JsonProperty("totalMermasImporte") BigDecimal totalWasteAmount,
        @JsonProperty("totalPedidos") Integer totalOrders,
        @JsonProperty("totalIngresos") BigDecimal totalIncome,
        @JsonProperty("totalEfectivo") BigDecimal totalCash,
        @JsonProperty("totalTransferencia") BigDecimal totalTransfers,
        @JsonProperty("totalPedidosYa") BigDecimal totalPedidosYa,
        @JsonProperty("variedadBajoStock") Integer lowStockVarieties,
        @JsonProperty("cantidadPedidosPY") Integer pedidosYaOrderCount,
        @JsonProperty("cantidadParticular") Integer directOrderCount,
        @JsonProperty("empanadasMasVendidas") List<SoldEmpanadaDTO> bestSellingEmpanadas,
        @JsonProperty("empanadasPerdidas") List<EmpanadaWasteDTO> lostEmpanadas

        ) {

}
