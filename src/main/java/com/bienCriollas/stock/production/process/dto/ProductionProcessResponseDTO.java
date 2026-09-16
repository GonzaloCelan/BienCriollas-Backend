package com.bienCriollas.stock.production.process.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductionProcessResponseDTO(
        Long id,
        Long varietyId,
        String varietyName,
        Integer version,
        Integer referenceYieldUnits,
        String notes,
        Integer stepCount,
        Integer totalEstimatedMinutes,
        Integer activeMinutes,
        Integer waitingMinutes,
        Integer estimatedPersonMinutes,
        BigDecimal estimatedPersonHours,
        Boolean active,
        List<ProductionProcessStepResponseDTO> steps,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
