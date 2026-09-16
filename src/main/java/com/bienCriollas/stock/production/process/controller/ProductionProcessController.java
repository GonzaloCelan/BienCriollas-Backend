package com.bienCriollas.stock.production.process.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bienCriollas.stock.production.process.dto.*;
import com.bienCriollas.stock.production.process.interfaces.IProductionProcessService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
@Tag(name = "Producción - Procesos",
        description = "Procesos estándar y versionados de elaboración por variedad.")
public class ProductionProcessController {

    private final IProductionProcessService processService;

    @PostMapping
    @Operation(summary = "Crear el primer proceso de una variedad")
    public ResponseEntity<ProductionProcessResponseDTO> createProcess(
            @Valid @RequestBody ProductionProcessRequestDTO dto) {
        ProductionProcessResponseDTO response = processService.createProcess(dto);
        return ResponseEntity.created(URI.create("/api/v1/processes/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Listar procesos vigentes")
    public ResponseEntity<Page<ProductionProcessResponseDTO>> getActiveProcesses(
            @ParameterObject
            @PageableDefault(size = 20, sort = {"varietyName", "id"})
            Pageable pageable) {
        return ResponseEntity.ok(processService.getActiveProcesses(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un proceso vigente o histórico")
    public ResponseEntity<ProductionProcessResponseDTO> getProcessById(
            @PathVariable Long id) {
        return ResponseEntity.ok(processService.getProcessById(id));
    }

    @GetMapping("/variety/{varietyId}")
    @Operation(summary = "Obtener el proceso vigente de una variedad")
    public ResponseEntity<ProductionProcessResponseDTO> getActiveProcessByVariety(
            @PathVariable Long varietyId) {
        return ResponseEntity.ok(processService.getActiveProcessByVariety(varietyId));
    }

    @GetMapping("/variety/{varietyId}/history")
    @Operation(summary = "Obtener el historial de procesos de una variedad")
    public ResponseEntity<List<ProductionProcessResponseDTO>> getProcessHistory(
            @PathVariable Long varietyId) {
        return ResponseEntity.ok(processService.getProcessHistory(varietyId));
    }

    @GetMapping("/status")
    @Operation(summary = "Listar procesos por estado")
    public ResponseEntity<Page<ProductionProcessResponseDTO>> getProcessesByStatus(
            @RequestParam Boolean active,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"varietyName", "id"})
            Pageable pageable) {
        return ResponseEntity.ok(processService.getProcessesByStatus(active, pageable));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Crear una nueva versión de un proceso")
    public ResponseEntity<ProductionProcessResponseDTO> createNewVersion(
            @PathVariable Long id,
            @Valid @RequestBody ProductionProcessVersionRequestDTO dto) {
        ProductionProcessResponseDTO response = processService.createNewVersion(id, dto);
        return ResponseEntity.created(URI.create("/api/v1/processes/" + response.id()))
                .body(response);
    }
}
