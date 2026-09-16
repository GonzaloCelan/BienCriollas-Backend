package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;

public record WasteReasonSummaryDTO(
        String reason,
        Integer totalUnits,
        BigDecimal percentage,
        BigDecimal estimatedCost
) {}
