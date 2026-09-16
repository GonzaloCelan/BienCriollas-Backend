package com.bienCriollas.stock.production.controller;

import java.net.URI;
import java.time.LocalDate;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bienCriollas.stock.production.dto.*;
import com.bienCriollas.stock.production.enums.ProductionStatus;
import com.bienCriollas.stock.production.interfaces.IProductionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productions")
@RequiredArgsConstructor
@Tag(name = "Producción - Producción real",
        description = "Tandas reales con consumo de ingredientes, stock final y métricas.")
public class ProductionController {

    private final IProductionService productionService;

    @PostMapping
    @Operation(summary = "Crear una producción en borrador")
    public ResponseEntity<ProductionResponseDTO> create(
            @Valid @RequestBody ProductionCreateRequestDTO dto) {
        ProductionResponseDTO response = productionService.createProduction(dto);
        return ResponseEntity.created(URI.create("/api/v1/productions/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Listar producciones")
    public ResponseEntity<Page<ProductionResponseDTO>> list(
            @ParameterObject @PageableDefault(size = 20, sort = {"productionDate", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productionService.getProductions(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener el detalle de una producción")
    public ResponseEntity<ProductionResponseDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.getProductionById(id));
    }

    @GetMapping("/status")
    @Operation(summary = "Listar producciones por estado")
    public ResponseEntity<Page<ProductionResponseDTO>> byStatus(
            @RequestParam ProductionStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = {"productionDate", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productionService.getProductionsByStatus(status, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Listar producciones por rango de fechas")
    public ResponseEntity<Page<ProductionResponseDTO>> byDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = {"productionDate", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(productionService.getProductionsByDateRange(from, to, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos reales de un borrador")
    public ResponseEntity<ProductionResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody ProductionUpdateDTO dto) {
        return ResponseEntity.ok(productionService.updateProduction(id, dto));
    }

    @PatchMapping("/{id}/ingredients")
    @Operation(summary = "Actualizar el consumo real de un ingrediente")
    public ResponseEntity<ProductionResponseDTO> updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody ProductionIngredientUpdateDTO dto) {
        return ResponseEntity.ok(productionService.updateIngredientConsumption(id, dto));
    }

    @PostMapping("/{id}/ingredients")
    @Operation(summary = "Agregar un ingrediente extra al borrador")
    public ResponseEntity<ProductionResponseDTO> addIngredient(
            @PathVariable Long id,
            @Valid @RequestBody ProductionIngredientUpdateDTO dto) {
        return ResponseEntity.ok(productionService.addExtraIngredient(id, dto));
    }

    @PostMapping("/{id}/finalize")
    @Operation(summary = "Finalizar y mover stock atómicamente")
    public ResponseEntity<ProductionResponseDTO> finalizeProduction(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.finalizeProduction(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar una producción en borrador")
    public ResponseEntity<ProductionResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.cancelProduction(id));
    }
}
