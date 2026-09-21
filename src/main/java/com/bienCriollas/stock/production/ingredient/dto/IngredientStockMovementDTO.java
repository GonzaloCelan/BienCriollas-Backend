package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record IngredientStockMovementDTO(
        @NotNull @DecimalMin(value = "0.0000", inclusive = false)
        @Digits(integer = 15, fraction = 4)
        BigDecimal quantity
) {}
