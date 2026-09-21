package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

public record RecipeCalculatedIngredientDTO(
        Long ingredientId,
        String ingredientName,
        MeasurementUnit measurementUnit,
        BigDecimal baseQuantity,
        BigDecimal requiredQuantity,
        BigDecimal currentStock,
        Boolean enoughStock,
        BigDecimal missingQuantity,
        BigDecimal estimatedCost
) {}
