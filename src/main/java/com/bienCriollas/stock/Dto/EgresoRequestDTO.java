package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bienCriollas.stock.enums.TipoEgreso;

public record EgresoRequestDTO(
		Long idCaja,
		TipoEgreso tipoEgreso,
		LocalDate fecha,
        String descripcion,
        BigDecimal monto
) {}
