package com.bienCriollas.stock.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.bienCriollas.stock.statistics.enums.ShiftType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PeakHourResponseDTO(
        @JsonProperty("periodo") PeriodDTO period,
        @JsonProperty("totalPedidosAnalizados") long totalOrders,
        @JsonProperty("totalMontoVendido") BigDecimal totalSales,
        @JsonProperty("horaPico") PeakSlotDTO peakSlot,
        @JsonProperty("turnos") ShiftsDTO shifts,
        @JsonProperty("pedidosFueraDeHorario") long outsideShiftOrders,
        @JsonProperty("montoFueraDeHorario") BigDecimal outsideShiftSales
) {
    public record PeakSlotDTO(
            @JsonProperty("inicio") @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonProperty("fin") @JsonFormat(pattern = "HH:mm") LocalTime end,
            @JsonProperty("pedidos") long orders,
            @JsonProperty("montoVendido") BigDecimal sales,
            @JsonProperty("porcentajeDelTotal") BigDecimal percentage,
            @JsonProperty("turno") ShiftType shift
    ) {}

    public record ShiftsDTO(
            @JsonProperty("mediodia") ShiftStatsDTO midday,
            @JsonProperty("noche") ShiftStatsDTO night
    ) {}

    public record ShiftStatsDTO(
            @JsonProperty("desde") @JsonFormat(pattern = "HH:mm") LocalTime from,
            @JsonProperty("hasta") @JsonFormat(pattern = "HH:mm") LocalTime until,
            @JsonProperty("totalPedidos") long totalOrders,
            @JsonProperty("totalMontoVendido") BigDecimal totalSales,
            @JsonProperty("franjas") List<TimeSlotDTO> slots
    ) {}

    public record TimeSlotDTO(
            @JsonProperty("inicio") @JsonFormat(pattern = "HH:mm") LocalTime start,
            @JsonProperty("fin") @JsonFormat(pattern = "HH:mm") LocalTime end,
            @JsonProperty("pedidos") long orders,
            @JsonProperty("montoVendido") BigDecimal sales,
            @JsonProperty("porcentajeDelTotal") BigDecimal percentage
    ) {}
}
