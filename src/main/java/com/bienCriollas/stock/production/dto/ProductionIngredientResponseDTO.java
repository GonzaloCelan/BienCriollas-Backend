package com.bienCriollas.stock.production.dto;

import java.math.BigDecimal;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

public record ProductionIngredientResponseDTO(
        Long ingredientId,
        String ingredientName,
        BigDecimal expectedQuantity,
        BigDecimal actualQuantity,
        BigDecimal differenceQuantity,
        BigDecimal differencePercentage,
        MeasurementUnit measurementUnit,
        BigDecimal costPerBaseUnitSnapshot,
        BigDecimal expectedCost,
        BigDecimal actualCost,
        BigDecimal currentStock,
        BigDecimal projectedStock,
        Boolean enoughStock) {
}
