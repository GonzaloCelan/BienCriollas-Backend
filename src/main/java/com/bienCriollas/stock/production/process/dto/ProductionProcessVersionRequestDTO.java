package com.bienCriollas.stock.production.process.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ProductionProcessVersionRequestDTO(
        @NotNull(message = "El rendimiento de referencia es obligatorio")
        @Min(value = 1, message = "El rendimiento de referencia debe ser mayor a 0")
        Integer referenceYieldUnits,

        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
        String notes,

        @NotEmpty(message = "El proceso debe contener al menos un paso")
        List<@Valid ProductionProcessStepRequestDTO> steps
) {}
