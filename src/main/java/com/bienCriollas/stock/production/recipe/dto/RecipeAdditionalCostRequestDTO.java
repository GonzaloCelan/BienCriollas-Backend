package com.bienCriollas.stock.production.recipe.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.production.recipe.enums.*;

import jakarta.validation.constraints.*;

public record RecipeAdditionalCostRequestDTO(
        @NotNull(message = "El tipo de costo es obligatorio")
        AdditionalCostType costType,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotNull(message = "El modo de cálculo es obligatorio")
        AdditionalCostCalculationMode calculationMode,

        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0.000001", message = "El valor debe ser mayor a cero")
        @Digits(integer = 13, fraction = 6,
                message = "El valor admite hasta 13 enteros y 6 decimales")
        BigDecimal value,

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 1, message = "El orden debe ser mayor a cero")
        Integer sortOrder,

        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String notes
) {}
