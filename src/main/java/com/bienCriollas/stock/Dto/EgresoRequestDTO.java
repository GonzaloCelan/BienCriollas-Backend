package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record EgresoRequestDTO(
		Long idCaja,
        String descripcion,
        BigDecimal monto
) {}
