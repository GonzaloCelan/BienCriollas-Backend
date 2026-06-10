
package com.bienCriollas.stock.Dto.egreso;

import java.math.BigDecimal;

import com.bienCriollas.stock.enums.TipoEgreso;

public record EgresosPorcentajeDTO(
		TipoEgreso tipoEgreso,
        BigDecimal totalMesActual,
        BigDecimal porcentaje) {

}
