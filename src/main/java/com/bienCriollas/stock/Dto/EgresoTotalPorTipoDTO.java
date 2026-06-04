package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.enums.TipoEgreso;

public record EgresoTotalPorTipoDTO(
        TipoEgreso tipoEgreso,
        BigDecimal total
) {
}