package com.bienCriollas.stock.production.dto;

import jakarta.validation.constraints.*;

public record ProductionUpdateDTO(
        @Min(0) Integer finalUnits,
        @Min(1) Integer totalMinutes,
        @Min(1) Integer peopleCount,
        @Min(0) Integer wasteUnits,
        @Size(max = 250) String wasteReason,
        @Size(max = 1000) String notes) {
}
