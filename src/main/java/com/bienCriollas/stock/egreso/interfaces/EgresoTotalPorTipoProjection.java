package com.bienCriollas.stock.egreso.interfaces;

import java.math.BigDecimal;

import com.bienCriollas.stock.egreso.enums.TipoEgreso;

public interface EgresoTotalPorTipoProjection {

    TipoEgreso getTipoEgreso();

    BigDecimal getTotal();
}