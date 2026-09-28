package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
import com.bienCriollas.stock.production.ingredient.enums.ReferencePriceUnit;

public record IngredientResponseDTO(
        Long id,
        String name,
        MeasurementUnit measurementUnit,
        String purchasePresentation,
        BigDecimal purchaseQuantity,
        BigDecimal purchasePrice,
        BigDecimal costPerBaseUnit,
        Boolean purchaseDataComplete,
        BigDecimal referencePrice,
        ReferencePriceUnit referencePriceUnit,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
