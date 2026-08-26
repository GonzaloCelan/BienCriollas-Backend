package com.bienCriollas.stock.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corrección manual del stock disponible.")
public record AjusteStockDTO(
        @Schema(description = "ID de la variedad.", example = "2") Long idVariedad,
        @Schema(description = "Nueva cantidad disponible.", example = "24") Integer stockDisponible
) {}
