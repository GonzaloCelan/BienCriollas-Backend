package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record RecipeIngredientRequestDTO(
        @NotNull(message = "El ingrediente es obligatorio")
        @Positive(message = "El ingrediente debe ser válido")
        Long ingredientId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0000", inclusive = false,
                message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 15, fraction = 4,
                message = "La cantidad admite hasta 15 enteros y 4 decimales")
        BigDecimal quantity
) {}
