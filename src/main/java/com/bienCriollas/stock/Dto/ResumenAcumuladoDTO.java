package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record ResumenAcumuladoDTO(
        BigDecimal acumuladoEfectivo,
        BigDecimal acumuladoTransferencia,
        BigDecimal acumuladoPedidosya,
        BigDecimal acumuladoTotal,
        BigDecimal egresoAcumulado,
        BigDecimal balanceFinal
) {}
