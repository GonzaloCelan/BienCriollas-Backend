package com.bienCriollas.stock.Dto;

import java.math.BigDecimal;

public record BalanceResponseDTO(
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal balance
) {}
