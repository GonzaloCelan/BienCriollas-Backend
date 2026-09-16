package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;

public record IngredientSummaryDTO(
        long totalIngredients,
        long activeIngredients,
        long inactiveIngredients,
        long lowStockIngredients,
        BigDecimal totalStockValue
) {}

