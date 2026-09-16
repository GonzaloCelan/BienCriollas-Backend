package com.bienCriollas.stock.production.analytics.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bienCriollas.stock.production.analytics.dto.*;
import com.bienCriollas.stock.production.analytics.interfaces.IProductionAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/production-analytics")
@RequiredArgsConstructor
@Tag(name = "Producción - Costos y rendimiento",
        description = "Análisis de costos, productividad, ingredientes y merma de producciones finalizadas.")
public class ProductionAnalyticsController {

    private final IProductionAnalyticsService analyticsService;

    @GetMapping("/settings")
    @Operation(summary = "Obtener la configuración vigente de costos")
    public ResponseEntity<ProductionCostSettingsResponseDTO> getSettings() {
        return ResponseEntity.ok(analyticsService.getSettings());
    }

    @PutMapping("/settings")
    @Operation(summary = "Actualizar la configuración de costos")
    public ResponseEntity<ProductionCostSettingsResponseDTO> updateSettings(
            @Valid @RequestBody ProductionCostSettingsRequestDTO dto) {
        return ResponseEntity.ok(analyticsService.updateSettings(dto));
    }

    @GetMapping("/summary")
    @Operation(summary = "Obtener el resumen de costos y rendimiento")
    public ResponseEntity<CostPerformanceSummaryDTO> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getSummary(from, to));
    }

    @GetMapping("/labor")
    @Operation(summary = "Obtener el resumen de rendimiento laboral")
    public ResponseEntity<LaborPerformanceSummaryDTO> getLaborPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getLaborPerformance(from, to));
    }

    @GetMapping("/productions/{productionId}")
    @Operation(summary = "Obtener el análisis de una producción finalizada")
    public ResponseEntity<ProductionCostDetailDTO> getProductionDetail(
            @PathVariable Long productionId) {
        return ResponseEntity.ok(analyticsService.getProductionDetail(productionId));
    }

    @GetMapping("/varieties")
    @Operation(summary = "Obtener el rendimiento agrupado por variedad")
    public ResponseEntity<List<VarietyPerformanceDTO>> getVarietyPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getVarietyPerformance(from, to));
    }

    @GetMapping("/varieties/{varietyId}")
    @Operation(summary = "Obtener el rendimiento de una variedad")
    public ResponseEntity<VarietyPerformanceDTO> getVarietyPerformance(
            @PathVariable Long varietyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                analyticsService.getVarietyPerformance(varietyId, from, to));
    }

    @GetMapping("/ingredients/deviations")
    @Operation(summary = "Obtener los desvíos de consumo por ingrediente")
    public ResponseEntity<List<IngredientDeviationDTO>> getIngredientDeviations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getIngredientDeviations(from, to));
    }

    @GetMapping("/waste")
    @Operation(summary = "Obtener la merma agrupada por motivo")
    public ResponseEntity<List<WasteReasonSummaryDTO>> getWasteSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getWasteSummary(from, to));
    }

    @GetMapping("/ranking/best")
    @Operation(summary = "Obtener las producciones con mejor productividad laboral")
    public ResponseEntity<List<ProductionRankingDTO>> getBestProductions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                analyticsService.getBestProductions(from, to, limit));
    }

    @GetMapping("/ranking/worst")
    @Operation(summary = "Obtener las producciones con mayor costo de ineficiencia")
    public ResponseEntity<List<ProductionRankingDTO>> getWorstProductions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                analyticsService.getWorstProductions(from, to, limit));
    }
}
