package com.bienCriollas.stock.production.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.bienCriollas.stock.production.enums.ProductionStatus;

public record ProductionResponseDTO(
        Long id,
        LocalDate productionDate,
        Long varietyId,
        String varietyName,
        Long recipeId,
        Integer recipeVersion,
        Long processId,
        Integer processVersion,
        Integer plannedUnits,
        Integer finalUnits,
        Integer wasteUnits,
        String wasteReason,
        Integer totalMinutes,
        Integer peopleCount,
        ProductionStatus status,
        String notes,
        List<ProductionIngredientResponseDTO> ingredients,
        List<ProductionAdditionalCostResponseDTO> additionalCosts,
        BigDecimal expectedIngredientCost,
        BigDecimal actualIngredientCost,
        BigDecimal actualIngredientCostPerUnit,
        BigDecimal standardUnitsPerHour,
        BigDecimal actualUnitsPerHour,
        BigDecimal productivityVariationPercentage,
        LocalDateTime createdAt,
        LocalDateTime finalizedAt) {
}
