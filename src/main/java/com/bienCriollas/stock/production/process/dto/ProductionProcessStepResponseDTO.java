package com.bienCriollas.stock.production.process.dto;

import com.bienCriollas.stock.production.process.enums.ProcessTimeType;

public record ProductionProcessStepResponseDTO(
        Long id,
        Integer stepOrder,
        String name,
        String description,
        Integer estimatedMinutes,
        Integer requiredPeople,
        ProcessTimeType timeType,
        String notes,
        Integer estimatedPersonMinutes
) {}
