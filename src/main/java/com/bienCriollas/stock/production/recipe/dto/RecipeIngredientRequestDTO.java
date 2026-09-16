package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record RecipeIngredientRequestDTO(
        @NotNull(message = "El ingrediente es obligatorio")
        @Positive(message = "El ingrediente debe ser válido")
        Long ingredientId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0 gramos")
        @Digits(integer = 12, fraction = 2, message = "La cantidad admite hasta 12 enteros y 2 decimales")
        BigDecimal quantityGrams
) {}

