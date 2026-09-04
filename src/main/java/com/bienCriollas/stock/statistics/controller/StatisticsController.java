package com.bienCriollas.stock.statistics.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;
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
