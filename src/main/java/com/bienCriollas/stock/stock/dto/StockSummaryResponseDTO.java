package com.bienCriollas.stock.stock.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StockSummaryResponseDTO(
        @JsonProperty("total_unidades_disponibles") Integer totalAvailableUnits,
        @JsonProperty("valor_total_stock") BigDecimal totalStockValue,
        @JsonProperty("variedades_con_stock") Integer varietiesWithStock,
        @JsonProperty("variedades_sin_valoracion") Integer varietiesWithoutValuation
) {}
