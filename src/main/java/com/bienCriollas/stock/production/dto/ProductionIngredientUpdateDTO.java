package com.bienCriollas.stock.production.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ProductionIngredientUpdateDTO(
        @NotNull Long ingredientId,
        @NotNull @DecimalMin("0.0000")
        @Digits(integer = 15, fraction = 4) BigDecimal actualQuantity) {
}
