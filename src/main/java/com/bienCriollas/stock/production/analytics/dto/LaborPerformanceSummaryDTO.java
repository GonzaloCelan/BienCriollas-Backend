package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;

public record LaborPerformanceSummaryDTO(
        BigDecimal totalPersonHours,
        BigDecimal expectedPersonHours,
        BigDecimal extraPersonHours,
        BigDecimal averageUnitsPerPersonHour,
        BigDecimal standardUnitsPerPersonHour,
        BigDecimal productivityVariationPercentage,
        BigDecimal actualLaborCost,
        BigDecimal expectedLaborCost,
        BigDecimal laborInefficiencyCost
) {}
