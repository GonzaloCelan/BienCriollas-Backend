package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record CajaEstadoDTO(
        BigDecimal ingresosEfectivo,
        BigDecimal ingresosTransferencias,
        BigDecimal ingresosPedidosYa,
        BigDecimal totalEgresos,
        BigDecimal totalMermas
) {}
