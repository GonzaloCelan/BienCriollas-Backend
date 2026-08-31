package com.bienCriollas.stock.seguridad.dto;

import com.bienCriollas.stock.seguridad.enums.RolUsuario;

import jakarta.validation.constraints.NotNull;

public record CambiarRolUsuarioRequestDTO(@NotNull RolUsuario rol) {
}
