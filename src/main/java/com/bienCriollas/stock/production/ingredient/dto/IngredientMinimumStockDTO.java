package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record IngredientMinimumStockDTO(
        @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4)
        BigDecimal minimumStock
) {}
