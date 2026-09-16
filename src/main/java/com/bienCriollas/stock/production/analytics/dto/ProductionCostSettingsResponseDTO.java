package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductionCostSettingsResponseDTO(
        BigDecimal averageHourlyLaborCost,
        BigDecimal energyPercentage,
        LocalDateTime updatedAt
) {}
