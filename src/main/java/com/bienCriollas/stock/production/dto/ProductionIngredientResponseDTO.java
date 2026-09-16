package com.bienCriollas.stock.production.dto;

import java.math.BigDecimal;

public record ProductionIngredientResponseDTO(
        Long ingredientId,
        String ingredientName,
        BigDecimal expectedQuantityGrams,
        BigDecimal actualQuantityGrams,
        BigDecimal differenceGrams,
        BigDecimal differencePercentage,
        BigDecimal costPerGramSnapshot,
        BigDecimal expectedCost,
        BigDecimal actualCost,
        BigDecimal currentStockGrams,
        BigDecimal projectedStockGrams,
        Boolean enoughStock) {
}
