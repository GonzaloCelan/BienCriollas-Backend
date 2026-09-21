package com.bienCriollas.stock.production.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.production.recipe.enums.*;

public record ProductionAdditionalCostResponseDTO(
        Long id,
        Long recipeAdditionalCostId,
        AdditionalCostType costType,
        String name,
        AdditionalCostCalculationMode calculationMode,
        BigDecimal value,
        BigDecimal expectedCost,
        Integer sortOrder
) {}
