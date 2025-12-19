
package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.Model.TipoEgreso;

public record EgresosPorcentajeDTO(
		TipoEgreso tipoEgreso,
        BigDecimal totalMesActual,
        BigDecimal porcentaje) {

}
