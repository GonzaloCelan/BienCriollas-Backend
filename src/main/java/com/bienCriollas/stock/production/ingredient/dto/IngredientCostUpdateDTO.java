package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record IngredientCostUpdateDTO(
        @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3)
        BigDecimal costPerKilogram
) {}
