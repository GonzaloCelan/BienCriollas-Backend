package com.bienCriollas.stock.variety.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Precios vigentes de una variedad.")
public record UpdatePriceDTO(

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
        @Schema(description = "Precio por unidad.", example = "1800.00")
        @JsonProperty("precioUnitario") BigDecimal unitPrice,

        @NotNull(message = "El precio de media docena es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de media docena debe ser mayor a 0")
        @Schema(description = "Precio por media docena.", example = "9500.00")
        @JsonProperty("precioMediaDocena") BigDecimal halfDozenPrice,

        @NotNull(message = "El precio de docena es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de docena debe ser mayor a 0")
        @Schema(description = "Precio por docena.", example = "18000.00")
        @JsonProperty("precioDocena") BigDecimal dozenPrice
) {
}
