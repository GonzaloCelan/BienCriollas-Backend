package com.bienCriollas.stock.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdatePaidStatusRequestDTO(
        @NotNull
        @JsonProperty("pagado")
        @Schema(description = "Nuevo estado operativo del cobro.", example = "true")
        Boolean pagado
) {}
