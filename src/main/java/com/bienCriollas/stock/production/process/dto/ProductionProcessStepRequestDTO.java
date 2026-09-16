package com.bienCriollas.stock.production.process.dto;

import com.bienCriollas.stock.production.process.enums.ProcessTimeType;

import jakarta.validation.constraints.*;

public record ProductionProcessStepRequestDTO(
        @NotBlank(message = "El nombre del paso es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,

        @NotNull(message = "El tiempo estimado es obligatorio")
        @Min(value = 1, message = "El tiempo estimado debe ser mayor a 0 minutos")
        Integer estimatedMinutes,

        @NotNull(message = "La cantidad de personas es obligatoria")
        @Min(value = 0, message = "La cantidad de personas no puede ser negativa")
        Integer requiredPeople,

        @NotNull(message = "El tipo de tiempo es obligatorio")
        ProcessTimeType timeType,

        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String notes
) {}
