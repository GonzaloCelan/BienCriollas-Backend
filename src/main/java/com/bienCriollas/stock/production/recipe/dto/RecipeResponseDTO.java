package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecipeResponseDTO(
        Long id,
        Long varietyId,
        String varietyName,
        Integer version,
        Integer baseYieldUnits,
        String notes,
        List<RecipeIngredientResponseDTO> ingredients,
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedCostPerUnit,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

