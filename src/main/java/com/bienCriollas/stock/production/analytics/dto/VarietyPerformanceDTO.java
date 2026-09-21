package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;

public record VarietyPerformanceDTO(
        Long varietyId,
        String varietyName,
        Long productionCount,
        Integer totalUnitsProduced,
        Integer totalWasteUnits,
        BigDecimal averageWastePercentage,
        BigDecimal totalIngredientCost,
        BigDecimal totalLaborCost,
        BigDecimal totalPackagingCost,
        BigDecimal totalOtherAdditionalCost,
        BigDecimal totalEnergyCost,
        BigDecimal totalProductionCost,
        BigDecimal averageCostPerUnit,
        BigDecimal averageUnitsPerHour,
        BigDecimal averageUnitsPerPersonHour,
        BigDecimal averageLaborProductivityVariation,
        BigDecimal laborInefficiencyCost,
        BigDecimal totalCostDeviation
) {}
