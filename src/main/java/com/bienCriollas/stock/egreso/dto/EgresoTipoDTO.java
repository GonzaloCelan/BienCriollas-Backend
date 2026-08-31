package com.bienCriollas.stock.egreso.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.egreso.enums.TipoEgreso;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos para registrar un egreso.")
public record EgresoTipoDTO(
		
		@Schema(description = "ID de caja, cuando corresponda.", example = "1", nullable = true) Long idCaja,
		@Schema(description = "Categoría del gasto.", example = "PRODUCCION") TipoEgreso tipoEgreso,
        @Schema(description = "Detalle del gasto.", example = "Compra de harina") String descripcion,
        @Schema(description = "Importe del egreso.", example = "25000.00") BigDecimal monto) {

}
