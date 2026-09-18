package com.bienCriollas.stock.statistics.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.statistics.dto.CustomerRankingResponseDTO;
import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO;
import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;
import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.bienCriollas.stock.statistics.enums.CustomerRankingOrder;
import com.bienCriollas.stock.statistics.service.StatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/estadisticas")
@RequiredArgsConstructor
@Tag(name = "Estadísticas", description = "Indicadores consolidados de la operación.")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/clientes-ranking")
    @Operation(summary = "Obtener ranking de clientes", description = "Particulares entregados agrupados por cliente y fecha comercial. DIA y ULTIMOS_7_DIAS requieren fecha; MES requiere mes (yyyy-MM). limit: 1 a 100.")
    public ResponseEntity<CustomerRankingResponseDTO> getCustomerRanking(
            @RequestParam("periodo") AnalysisPeriod period,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "mes", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(value = "orden", defaultValue = "IMPORTE") CustomerRankingOrder order,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(statisticsService.getCustomerRanking(period, date, month, order, limit));
    }

    @GetMapping("/hora-pico")
    @Operation(summary = "Obtener la hora pico del negocio", description = "Agrupa los pedidos entregados por su hora real de creación en franjas de 30 minutos. DIA y ULTIMOS_7_DIAS requieren fecha; MES requiere mes (yyyy-MM).")
    public ResponseEntity<PeakHourResponseDTO> getPeakHour(
            @RequestParam("periodo") AnalysisPeriod period,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "mes", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(statisticsService.getPeakHour(period, date, month));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen estadístico", description = "Calcula métricas de ventas, ingresos, egresos y mermas entre dos fechas.")
    public ResponseEntity<StatisticsSummaryDTO> getSummary(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end
    ) {
        return ResponseEntity.ok(
                statisticsService.getSummary(start, end)
        );
    }
}
