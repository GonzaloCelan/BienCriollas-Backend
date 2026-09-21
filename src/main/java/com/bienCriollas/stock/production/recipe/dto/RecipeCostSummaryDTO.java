package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

public record RecipeCostSummaryDTO(
        BigDecimal ingredientCost,
        BigDecimal fixedAdditionalCost,
        BigDecimal perUnitAdditionalCost,
        BigDecimal subtotalBeforePercentage,
        BigDecimal percentageAdditionalCost,
        BigDecimal totalAdditionalCost,
        BigDecimal estimatedRecipeTotalCost,
        BigDecimal estimatedCostPerUnit
) {}
