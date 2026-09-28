package com.bienCriollas.stock.production.ingredient.dto;

public record IngredientSummaryDTO(
        long totalIngredients,
        long activeIngredients,
        long inactiveIngredients
) {}
