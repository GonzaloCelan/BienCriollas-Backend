package com.bienCriollas.stock.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotBlank @Size(min = 10, max = 72) String password) {
}
