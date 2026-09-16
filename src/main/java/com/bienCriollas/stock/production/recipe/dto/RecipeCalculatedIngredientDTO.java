package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

public record RecipeCalculatedIngredientDTO(
        Long ingredientId,
        String ingredientName,
        BigDecimal baseQuantityGrams,
        BigDecimal requiredQuantityGrams,
        BigDecimal currentStockGrams,
        Boolean enoughStock,
        BigDecimal missingGrams,
        BigDecimal estimatedCost
) {}

