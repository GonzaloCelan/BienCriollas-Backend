package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bienCriollas.stock.Model.TipoEgreso;

public record EgresoTipoDTO(
		
		Long idCaja,
		TipoEgreso tipoEgreso,
        String descripcion,
        BigDecimal monto) {

}
