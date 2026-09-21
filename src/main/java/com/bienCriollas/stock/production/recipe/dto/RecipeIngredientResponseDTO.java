package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

public record RecipeIngredientResponseDTO(
        Long ingredientId,
        String ingredientName,
        BigDecimal quantity,
        MeasurementUnit measurementUnit,
        BigDecimal currentCostPerBaseUnit,
        BigDecimal estimatedCost
) {}
