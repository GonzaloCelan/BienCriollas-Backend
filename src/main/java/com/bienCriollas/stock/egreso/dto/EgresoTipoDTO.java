package com.bienCriollas.stock.egreso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bienCriollas.stock.egreso.enums.TipoEgreso;

public record EgresoTipoDTO(
		
		Long idCaja,
		TipoEgreso tipoEgreso,
        String descripcion,
        BigDecimal monto) {

}
