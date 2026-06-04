package com.bienCriollas.stock.Interface;

import java.math.BigDecimal;

import com.bienCriollas.stock.enums.TipoEgreso;

public interface EgresoTotalPorTipoProjection {

    TipoEgreso getTipoEgreso();

    BigDecimal getTotal();
}