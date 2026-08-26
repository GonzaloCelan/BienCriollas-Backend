package com.bienCriollas.stock.merma.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unidades perdidas o descartadas de una variedad.")
public record PerdidaEmpanadaDTO(
        @Schema(description = "ID de la variedad.", example = "2") Long idVariedad,
        @Schema(description = "Cantidad perdida.", example = "3") Integer cantidad
        
) {}
