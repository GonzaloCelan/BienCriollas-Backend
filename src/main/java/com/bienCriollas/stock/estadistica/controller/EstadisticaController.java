package com.bienCriollas.stock.estadistica.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bienCriollas.stock.estadistica.dto.EstadisticaResumenDTO;
import com.bienCriollas.stock.estadistica.service.EstadisticaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/estadisticas")
@RequiredArgsConstructor
@Tag(name = "Estadísticas", description = "Indicadores consolidados de la operación.")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen estadístico", description = "Calcula métricas de ventas, ingresos, egresos y mermas entre dos fechas.")
    public ResponseEntity<EstadisticaResumenDTO> obtenerResumen(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta
    ) {
        return ResponseEntity.ok(
                estadisticaService.obtenerResumen(desde, hasta)
        );
    }
}
