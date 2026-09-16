package com.bienCriollas.stock.production.analytics.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record ProductionCostSettingsRequestDTO(
        @NotNull(message = "El costo laboral promedio es obligatorio")
        @DecimalMin(value = "0.01", message = "El costo laboral promedio debe ser mayor a 0")
        @Digits(integer = 12, fraction = 2,
                message = "El costo laboral promedio admite hasta 12 enteros y 2 decimales")
        BigDecimal averageHourlyLaborCost,

        @NotNull(message = "El porcentaje de energía es obligatorio")
        @DecimalMin(value = "0.00", message = "El porcentaje de energía no puede ser negativo")
        @DecimalMax(value = "100.00", message = "El porcentaje de energía no puede superar 100")
        @Digits(integer = 3, fraction = 2,
                message = "El porcentaje de energía admite hasta 3 enteros y 2 decimales")
        BigDecimal energyPercentage
) {}
