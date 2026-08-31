package com.bienCriollas.stock.seguridad.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponseDTO usuario) {
}
