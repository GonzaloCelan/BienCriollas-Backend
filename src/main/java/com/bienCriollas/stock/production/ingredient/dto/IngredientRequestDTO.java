package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record IngredientRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal currentStockGrams,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal minimumStockGrams,
        @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal costPerKilogram
) {}
