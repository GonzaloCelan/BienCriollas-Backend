package com.bienCriollas.stock.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequestDTO(@JsonProperty("activo") @NotNull Boolean active) {
}
