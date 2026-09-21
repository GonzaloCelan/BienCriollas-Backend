package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

public record IngredientDeviationDTO(
        Long ingredientId,
        String ingredientName,
        MeasurementUnit measurementUnit,
        BigDecimal totalExpectedQuantity,
        BigDecimal totalActualQuantity,
        BigDecimal differenceQuantity,
        BigDecimal differencePercentage,
        BigDecimal additionalCost
) {}
