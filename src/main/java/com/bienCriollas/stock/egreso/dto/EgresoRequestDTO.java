package com.bienCriollas.stock.egreso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bienCriollas.stock.egreso.enums.TipoEgreso;

public record EgresoRequestDTO(
		Long idCaja,
		TipoEgreso tipoEgreso,
		LocalDate fecha,
        String descripcion,
        BigDecimal monto
) {}
