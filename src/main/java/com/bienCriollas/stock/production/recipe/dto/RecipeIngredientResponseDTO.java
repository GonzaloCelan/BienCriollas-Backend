package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

public record RecipeIngredientResponseDTO(
        Long ingredientId,
        String ingredientName,
        BigDecimal quantityGrams,
        BigDecimal costPerGram,
        BigDecimal estimatedCost
) {}

