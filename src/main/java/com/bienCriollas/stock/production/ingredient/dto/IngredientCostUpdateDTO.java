package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record IngredientCostUpdateDTO(
        @NotBlank @Size(max = 100) String purchasePresentation,
        @NotNull @DecimalMin(value = "0", inclusive = false,
                message = "La cantidad de la presentación de compra debe ser mayor a cero.")
        @Digits(integer = 15, fraction = 4) BigDecimal purchaseQuantity,
        @NotNull @DecimalMin(value = "0", inclusive = false,
                message = "El precio de la presentación de compra debe ser mayor a cero.")
        @Digits(integer = 17, fraction = 2) BigDecimal purchasePrice
) {}
