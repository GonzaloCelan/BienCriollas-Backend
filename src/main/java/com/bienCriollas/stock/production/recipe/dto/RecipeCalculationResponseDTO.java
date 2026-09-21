package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeCalculationResponseDTO(
        Long recipeId,
        Long varietyId,
        String varietyName,
        Integer recipeVersion,
        Integer baseYieldUnits,
        Integer requestedUnits,
        BigDecimal scaleFactor,
        List<RecipeCalculatedIngredientDTO> ingredients,
        List<RecipeAdditionalCostResponseDTO> additionalCosts,
        RecipeCostSummaryDTO costSummary,
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedCostPerUnit
) {}
