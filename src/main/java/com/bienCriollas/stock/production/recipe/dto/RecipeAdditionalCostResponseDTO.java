package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.production.recipe.enums.*;

public record RecipeAdditionalCostResponseDTO(
        Long id,
        AdditionalCostType costType,
        String name,
        AdditionalCostCalculationMode calculationMode,
        BigDecimal value,
        BigDecimal calculatedCost,
        Integer sortOrder,
        String notes
) {}
