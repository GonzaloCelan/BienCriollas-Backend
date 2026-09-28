package com.bienCriollas.stock.production.ingredient.controller;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Producción - Ingredientes", description = "Catálogo de materias primas, presentaciones de compra y costos.")
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
    @Deprecated
    @Operation(summary = "Endpoint discontinuado: el stock de ingredientes ya no se controla", deprecated = true)
    public ResponseEntity<Void> getLowStockIngredients() {
        return ResponseEntity.status(HttpStatus.GONE).build();
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
    @Deprecated
    @Operation(summary = "Endpoint discontinuado: el stock de ingredientes ya no se controla", deprecated = true)
    public ResponseEntity<Void> setStock(@PathVariable Long id, @RequestBody(required = false) Object ignored) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @PatchMapping("/{id}/stock/increase")
    @Deprecated
    @Operation(summary = "Endpoint discontinuado: el stock de ingredientes ya no se controla", deprecated = true)
    public ResponseEntity<Void> increaseStock(@PathVariable Long id, @RequestBody(required = false) Object ignored) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @PatchMapping("/{id}/stock/decrease")
    @Deprecated
    @Operation(summary = "Endpoint discontinuado: el stock de ingredientes ya no se controla", deprecated = true)
    public ResponseEntity<Void> decreaseStock(@PathVariable Long id, @RequestBody(required = false) Object ignored) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @PatchMapping("/{id}/cost")
    @Operation(summary = "Actualizar presentación y precio de compra; el costo base se recalcula")
    public ResponseEntity<IngredientResponseDTO> updateCost(@PathVariable Long id, @Valid @RequestBody IngredientCostUpdateDTO dto) {
        return ResponseEntity.ok(ingredientService.updateCost(id, dto));
    }

    @PatchMapping("/{id}/minimum-stock")
    @Deprecated
    @Operation(summary = "Endpoint discontinuado: el stock mínimo ya no se controla", deprecated = true)
    public ResponseEntity<Void> updateMinimumStock(@PathVariable Long id, @RequestBody(required = false) Object ignored) {
        return ResponseEntity.status(HttpStatus.GONE).build();
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
