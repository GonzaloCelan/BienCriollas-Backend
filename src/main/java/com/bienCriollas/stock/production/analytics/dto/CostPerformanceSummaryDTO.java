package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CostPerformanceSummaryDTO(
        LocalDate from,
        LocalDate to,
        Long totalProductions,
        Integer totalUnitsProduced,
        Integer totalWasteUnits,
        BigDecimal totalIngredientCost,
        BigDecimal totalLaborCost,
        BigDecimal totalPackagingCost,
        BigDecimal totalOtherAdditionalCost,
        BigDecimal totalEnergyCost,
        BigDecimal totalProductionCost,
        BigDecimal averageCostPerUnit,
        BigDecimal totalPersonHours,
        BigDecimal averageUnitsPerPersonHour,
        BigDecimal averageProductivityVariationPercentage,
        BigDecimal totalLaborInefficiencyCost,
        BigDecimal totalEstimatedWasteCost,
        Long efficientProductions,
        Long inefficientProductions
) {}
