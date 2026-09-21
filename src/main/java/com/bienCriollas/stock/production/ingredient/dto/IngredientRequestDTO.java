package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
import jakarta.validation.constraints.*;

public record IngredientRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @NotNull MeasurementUnit measurementUnit,
        @Size(max = 100) String purchasePresentation,
        @DecimalMin(value = "0", inclusive = false,
                message = "La cantidad de la presentación de compra debe ser mayor a cero.")
        @Digits(integer = 15, fraction = 4) BigDecimal purchaseQuantity,
        @DecimalMin(value = "0", inclusive = false,
                message = "El precio de la presentación de compra debe ser mayor a cero.")
        @Digits(integer = 17, fraction = 2) BigDecimal purchasePrice,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal currentStock,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal minimumStock
) {}
