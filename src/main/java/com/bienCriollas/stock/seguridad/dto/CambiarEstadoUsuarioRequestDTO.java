package com.bienCriollas.stock.seguridad.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequestDTO(@NotNull Boolean activo) {
}
