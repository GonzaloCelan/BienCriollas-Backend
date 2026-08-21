package com.bienCriollas.stock.variedad.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ActualizarPrecioDTO(

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
        BigDecimal precioUnitario,

        @NotNull(message = "El precio de media docena es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de media docena debe ser mayor a 0")
        BigDecimal precioMediaDocena,

        @NotNull(message = "El precio de docena es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de docena debe ser mayor a 0")
        BigDecimal precioDocena
) {
}