package com.bienCriollas.stock.production.ingredient.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IngredientResponseDTO(
        Long id,
        String name,
        BigDecimal currentStockGrams,
        BigDecimal minimumStockGrams,
        BigDecimal costPerGram,
        BigDecimal costPerKilogram,
        BigDecimal stockValue,
        Boolean lowStock,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
