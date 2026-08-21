package com.bienCriollas.stock.egreso.dto;

import java.math.BigDecimal;

import com.bienCriollas.stock.egreso.enums.TipoEgreso;

public record EgresoTotalPorTipoDTO(
        TipoEgreso tipoEgreso,
        BigDecimal total
) {
}