package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

public record IngredientResponseDTO(
        Long id,
        String name,
        MeasurementUnit measurementUnit,
        String purchasePresentation,
        BigDecimal purchaseQuantity,
        BigDecimal purchasePrice,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal costPerBaseUnit,
        Boolean purchaseDataComplete,
        BigDecimal stockValue,
        Boolean lowStock,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
