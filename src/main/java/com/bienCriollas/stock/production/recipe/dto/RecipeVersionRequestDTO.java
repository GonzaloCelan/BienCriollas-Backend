package com.bienCriollas.stock.production.recipe.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RecipeVersionRequestDTO(
        @NotNull(message = "El rendimiento base es obligatorio")
        @Min(value = 1, message = "El rendimiento debe ser mayor a 0")
        Integer baseYieldUnits,

        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String notes,

        @NotEmpty(message = "La receta debe contener al menos un ingrediente")
        List<@Valid RecipeIngredientRequestDTO> ingredients,

        List<@Valid RecipeAdditionalCostRequestDTO> additionalCosts
) {}
