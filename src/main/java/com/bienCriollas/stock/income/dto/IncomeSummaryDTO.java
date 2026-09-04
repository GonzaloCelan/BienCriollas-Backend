package com.bienCriollas.stock.income.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IncomeSummaryDTO(
        @JsonProperty("efectivo") BigDecimal cash,
        @JsonProperty("transferencia") BigDecimal transfer,
        @JsonProperty("pedidosYaEstimado") BigDecimal estimatedPedidosYa,
        @JsonProperty("liquidacionesPedidosYa") BigDecimal pedidosYaSettlements,

        @JsonProperty("totalRealRecibido") BigDecimal totalActuallyReceived,
        @JsonProperty("acumuladoPeriodo") BigDecimal accumulatedPeriod,

        @JsonProperty("cantidadPedidosParticulares") Integer directOrderCount,
        @JsonProperty("cantidadPedidosYa") Integer pedidosYaOrderCount,

        @JsonProperty("ingresosPorDia") List<IncomeByDayDTO> incomeByDay,
        @JsonProperty("distribucion") List<IncomeDistributionDTO> distribution,
        @JsonProperty("movimientos") List<IncomeMovementDTO> movements
) {

    public record IncomeByDayDTO(
            @JsonProperty("fecha") LocalDate date,
            @JsonProperty("efectivo") BigDecimal cash,
            @JsonProperty("transferencia") BigDecimal transfer,
            @JsonProperty("pedidosYaEstimado") BigDecimal estimatedPedidosYa,
            @JsonProperty("total") BigDecimal total
    ) {
    }

    public record IncomeDistributionDTO(
            @JsonProperty("tipo") String type,
            @JsonProperty("monto") BigDecimal amount,
            @JsonProperty("porcentaje") BigDecimal percentage
    ) {
    }

    public record IncomeMovementDTO(
            @JsonProperty("id") Long id,
            @JsonProperty("idPedido") Long orderId,
            @JsonProperty("fecha") LocalDate date,
            @JsonProperty("fechaHora") LocalDateTime dateTime,
            @JsonProperty("descripcion") String description,
            @JsonProperty("tipoVenta") String saleType,
            @JsonProperty("medioPago") String paymentMethod,
            @JsonProperty("monto") BigDecimal amount,
            @JsonProperty("estadoIngreso") String incomeStatus,
            @JsonProperty("origen") String source
    ) {
    }
}
