package com.bienCriollas.stock.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequestDTO(
        @NotBlank @Size(min = 10, max = 72) String password) {
}
