package com.bienCriollas.stock.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMyPasswordRequestDTO(
        @JsonProperty("passwordActual") @NotBlank String currentPassword,
        @JsonProperty("passwordNueva") @NotBlank @Size(min = 10, max = 72) String newPassword) {
}
