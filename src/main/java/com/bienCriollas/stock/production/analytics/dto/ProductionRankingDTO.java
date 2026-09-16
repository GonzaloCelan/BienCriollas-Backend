package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductionRankingDTO(
        Long productionId,
        LocalDate productionDate,
        String varietyName,
        Integer finalUnits,
        BigDecimal actualUnitsPerPersonHour,
        BigDecimal productivityVariationPercentage,
        BigDecimal laborInefficiencyCost,
        BigDecimal actualCostPerUnit
) {}
