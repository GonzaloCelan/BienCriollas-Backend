package com.bienCriollas.stock.order.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReconcileOrdersRequestDTO(
        @JsonProperty("anio") @NotNull @Min(2000) @Max(2100) Integer year,
        @JsonProperty("mes") @NotNull @Min(1) @Max(12) Integer month,
        @JsonProperty("idsPedidos") @NotEmpty @Size(max = 200) List<@NotNull Long> orderIds,
        @JsonProperty("motivo") @NotBlank @Size(max = 250) String reason,
        @JsonProperty("confirmar") @NotNull @AssertTrue(message = "Debés confirmar expresamente la regularización") Boolean confirmed) {
}
