package com.bienCriollas.stock.production.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ProductionIngredientUpdateDTO(
        @NotNull Long ingredientId,
        @NotNull @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 12, fraction = 2) BigDecimal actualQuantityGrams) {
}
