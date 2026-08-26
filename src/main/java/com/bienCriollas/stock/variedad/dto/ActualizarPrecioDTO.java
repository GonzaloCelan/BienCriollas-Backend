package com.bienCriollas.stock.variedad.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Precios vigentes de una variedad.")
public record ActualizarPrecioDTO(

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
        @Schema(description = "Precio por unidad.", example = "1800.00")
        BigDecimal precioUnitario,

        @NotNull(message = "El precio de media docena es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de media docena debe ser mayor a 0")
        @Schema(description = "Precio por media docena.", example = "9500.00")
        BigDecimal precioMediaDocena,

        @NotNull(message = "El precio de docena es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de docena debe ser mayor a 0")
        @Schema(description = "Precio por docena.", example = "18000.00")
        BigDecimal precioDocena
) {
}
