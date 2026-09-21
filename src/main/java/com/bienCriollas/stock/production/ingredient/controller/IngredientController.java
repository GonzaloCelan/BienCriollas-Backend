package com.bienCriollas.stock.production.ingredient.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.bienCriollas.stock.production.ingredient.dto.*;
import com.bienCriollas.stock.production.ingredient.interfaces.IIngredientService;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
@Tag(name = "Producción - Ingredientes", description = "Stock y costos en la unidad base de cada materia prima.")
public class IngredientController {

    private final IIngredientService ingredientService;

    @PostMapping
    @Operation(summary = "Crear ingrediente")
    public ResponseEntity<IngredientResponseDTO> createIngredient(@Valid @RequestBody IngredientRequestDTO dto) {
        IngredientResponseDTO response = ingredientService.createIngredient(dto);
        return ResponseEntity.created(URI.create("/api/v1/ingredients/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar ingredientes activos")
    public ResponseEntity<Page<IngredientResponseDTO>> getIngredients(@ParameterObject @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(ingredientService.getIngredients(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ingrediente por id")
    public ResponseEntity<IngredientResponseDTO> getIngredientById(@PathVariable Long id) {
        return ResponseEntity.ok(ingredientService.getIngredientById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar ingredientes activos por nombre")
    public ResponseEntity<Page<IngredientResponseDTO>> searchIngredients(@RequestParam String query, @ParameterObject @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(ingredientService.searchIngredients(query, pageable));
    }

    @GetMapping("/status")
    @Operation(summary = "Listar ingredientes por estado")
    public ResponseEntity<Page<IngredientResponseDTO>> getIngredientsByStatus(@RequestParam Boolean active, @ParameterObject @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(ingredientService.getIngredientsByStatus(active, pageable));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Listar ingredientes activos con stock bajo")
    public ResponseEntity<List<IngredientResponseDTO>> getLowStockIngredients() {
        return ResponseEntity.ok(ingredientService.getLowStockIngredients());
    }

    @GetMapping("/summary")
    @Operation(summary = "Obtener resumen general de ingredientes")
    public ResponseEntity<IngredientSummaryDTO> getSummary() {
        return ResponseEntity.ok(ingredientService.getSummary());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ingrediente")
    public ResponseEntity<IngredientResponseDTO> updateIngredient(@PathVariable Long id, @Valid @RequestBody IngredientRequestDTO dto) {
        return ResponseEntity.ok(ingredientService.updateIngredient(id, dto));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Establecer stock exacto")
    public ResponseEntity<IngredientResponseDTO> setStock(@PathVariable Long id, @Valid @RequestBody IngredientStockUpdateDTO dto) {
        return ResponseEntity.ok(ingredientService.setStock(id, dto));
    }

    @PatchMapping("/{id}/stock/increase")
    @Operation(summary = "Sumar stock en la unidad base del ingrediente")
    public ResponseEntity<IngredientResponseDTO> increaseStock(@PathVariable Long id, @Valid @RequestBody IngredientStockMovementDTO dto) {
        return ResponseEntity.ok(ingredientService.increaseStock(id, dto));
    }

    @PatchMapping("/{id}/stock/decrease")
    @Operation(summary = "Descontar stock en la unidad base del ingrediente")
    public ResponseEntity<IngredientResponseDTO> decreaseStock(@PathVariable Long id, @Valid @RequestBody IngredientStockMovementDTO dto) {
        return ResponseEntity.ok(ingredientService.decreaseStock(id, dto));
    }

    @PatchMapping("/{id}/cost")
    @Operation(summary = "Actualizar presentación y precio de compra; el costo base se recalcula")
    public ResponseEntity<IngredientResponseDTO> updateCost(@PathVariable Long id, @Valid @RequestBody IngredientCostUpdateDTO dto) {
        return ResponseEntity.ok(ingredientService.updateCost(id, dto));
    }

    @PatchMapping("/{id}/minimum-stock")
    @Operation(summary = "Actualizar stock mínimo")
    public ResponseEntity<IngredientResponseDTO> updateMinimumStock(@PathVariable Long id, @Valid @RequestBody IngredientMinimumStockDTO dto) {
        return ResponseEntity.ok(ingredientService.updateMinimumStock(id, dto));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar ingrediente")
    public ResponseEntity<IngredientResponseDTO> activateIngredient(@PathVariable Long id) {
        return ResponseEntity.ok(ingredientService.activateIngredient(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar ingrediente conservando sus datos")
    public ResponseEntity<IngredientResponseDTO> deactivateIngredient(@PathVariable Long id) {
        return ResponseEntity.ok(ingredientService.deactivateIngredient(id));
    }
}
