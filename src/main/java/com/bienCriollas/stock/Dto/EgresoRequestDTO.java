package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EgresoRequestDTO(
		Long idCaja,
		LocalDate fecha,
        String descripcion,
        BigDecimal monto
) {}
