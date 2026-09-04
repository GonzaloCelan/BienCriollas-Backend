package com.bienCriollas.stock.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        @JsonProperty("usuario") UserResponseDTO user) {
}
