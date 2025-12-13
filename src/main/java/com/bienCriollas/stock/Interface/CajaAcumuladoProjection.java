package com.bienCriollas.stock.Interface;

import java.math.BigDecimal;

public interface CajaAcumuladoProjection {

	BigDecimal getAcumuladoEfectivo();
    BigDecimal getAcumuladoTransferencia();
    BigDecimal getAcumuladoPedidosya();
    BigDecimal getAcumuladoTotal();
    BigDecimal getEgresoAcumulado();
}
