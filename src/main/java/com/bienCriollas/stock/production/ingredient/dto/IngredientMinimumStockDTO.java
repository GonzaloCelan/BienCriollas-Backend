package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record IngredientMinimumStockDTO(
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
        BigDecimal minimumStockGrams
) {}

