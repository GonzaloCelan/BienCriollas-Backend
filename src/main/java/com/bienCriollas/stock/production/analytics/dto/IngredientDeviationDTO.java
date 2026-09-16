package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;

public record IngredientDeviationDTO(
        Long ingredientId,
        String ingredientName,
        BigDecimal totalExpectedGrams,
        BigDecimal totalActualGrams,
        BigDecimal differenceGrams,
        BigDecimal differencePercentage,
        BigDecimal additionalCost
) {}
