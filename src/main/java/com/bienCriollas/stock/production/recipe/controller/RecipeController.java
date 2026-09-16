package com.bienCriollas.stock.production.recipe.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bienCriollas.stock.production.recipe.dto.*;
import com.bienCriollas.stock.production.recipe.interfaces.IRecipeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@Tag(name = "Producción - Recetas",
        description = "Composición estándar y versionada de cada variedad.")
public class RecipeController {

    private final IRecipeService recipeService;

    @PostMapping
    @Operation(summary = "Crear la primera receta de una variedad")
    public ResponseEntity<RecipeResponseDTO> createRecipe(
            @Valid @RequestBody RecipeRequestDTO dto) {
        RecipeResponseDTO response = recipeService.createRecipe(dto);
        return ResponseEntity.created(URI.create("/api/v1/recipes/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Listar recetas activas")
    public ResponseEntity<Page<RecipeResponseDTO>> getActiveRecipes(
            @ParameterObject
            @PageableDefault(size = 20, sort = {"varietyName", "id"})
            Pageable pageable) {
        return ResponseEntity.ok(recipeService.getActiveRecipes(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una receta activa o histórica")
    public ResponseEntity<RecipeResponseDTO> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    @GetMapping("/variety/{varietyId}")
    @Operation(summary = "Obtener la receta activa de una variedad")
    public ResponseEntity<RecipeResponseDTO> getActiveRecipeByVariety(
            @PathVariable Long varietyId) {
        return ResponseEntity.ok(recipeService.getActiveRecipeByVariety(varietyId));
    }

    @GetMapping("/variety/{varietyId}/history")
    @Operation(summary = "Obtener el historial de recetas de una variedad")
    public ResponseEntity<List<RecipeResponseDTO>> getRecipeHistory(
            @PathVariable Long varietyId) {
        return ResponseEntity.ok(recipeService.getRecipeHistory(varietyId));
    }

    @GetMapping("/status")
    @Operation(summary = "Listar recetas por estado")
    public ResponseEntity<Page<RecipeResponseDTO>> getRecipesByStatus(
            @RequestParam Boolean active,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"varietyName", "id"})
            Pageable pageable) {
        return ResponseEntity.ok(recipeService.getRecipesByStatus(active, pageable));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Crear una nueva versión de una receta")
    public ResponseEntity<RecipeResponseDTO> createNewVersion(
            @PathVariable Long id,
            @Valid @RequestBody RecipeVersionRequestDTO dto) {
        RecipeResponseDTO response = recipeService.createNewVersion(id, dto);
        return ResponseEntity.created(URI.create("/api/v1/recipes/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}/calculate")
    @Operation(summary = "Calcular ingredientes para una cantidad",
            description = "Simula cantidades, costos y faltantes sin modificar stock.")
    public ResponseEntity<RecipeCalculationResponseDTO> calculateRecipe(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(recipeService.calculateRecipe(id, quantity));
    }
}

