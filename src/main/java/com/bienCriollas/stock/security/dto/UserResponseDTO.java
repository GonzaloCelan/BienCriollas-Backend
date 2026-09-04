package com.bienCriollas.stock.security.dto;

import java.time.OffsetDateTime;

import com.bienCriollas.stock.security.entity.UserAccount;
import com.bienCriollas.stock.security.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponseDTO(
        Long id,
        @JsonProperty("nombre") String name,
        String username,
        @JsonProperty("rol") UserRole role,
        @JsonProperty("activo") boolean active,
        @JsonProperty("creadoEn") OffsetDateTime createdAt) {

    public static UserResponseDTO from(UserAccount user) {
        return new UserResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
