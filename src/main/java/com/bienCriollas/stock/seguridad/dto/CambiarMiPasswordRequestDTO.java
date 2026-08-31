package com.bienCriollas.stock.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarMiPasswordRequestDTO(
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 10, max = 72) String passwordNueva) {
}
