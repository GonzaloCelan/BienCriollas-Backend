package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record IngredientStockUpdateDTO(
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
        BigDecimal stockGrams
) {}

