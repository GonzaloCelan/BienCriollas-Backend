package com.bienCriollas.stock.security.dto;

import com.bienCriollas.stock.security.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @JsonProperty("nombre") @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 3, max = 80)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario sólo puede contener letras, números, punto, guion y guion bajo")
        String username,
        @NotBlank @Size(min = 10, max = 72) String password,
        @JsonProperty("rol") @NotNull UserRole role) {
}
