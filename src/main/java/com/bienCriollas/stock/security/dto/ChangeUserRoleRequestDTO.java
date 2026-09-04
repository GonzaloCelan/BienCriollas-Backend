package com.bienCriollas.stock.security.dto;

import com.bienCriollas.stock.security.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequestDTO(@JsonProperty("rol") @NotNull UserRole role) {
}
