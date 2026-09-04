package com.bienCriollas.stock.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(max = 200) String password) {
}
