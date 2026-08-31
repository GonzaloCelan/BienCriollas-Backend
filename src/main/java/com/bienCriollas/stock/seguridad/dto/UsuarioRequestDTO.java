package com.bienCriollas.stock.seguridad.dto;

import com.bienCriollas.stock.seguridad.enums.RolUsuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(min = 3, max = 80)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario sólo puede contener letras, números, punto, guion y guion bajo")
        String username,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotNull RolUsuario rol) {
}
