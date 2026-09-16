package com.bienCriollas.stock.production.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record ProductionCreateRequestDTO(
        @NotNull Long varietyId,
        @NotNull LocalDate productionDate,
        @NotNull @Min(1) Integer plannedUnits,
        @Size(max = 1000) String notes) {
}
