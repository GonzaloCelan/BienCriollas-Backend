package com.bienCriollas.stock.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StockActualResponseDTO(
        @JsonProperty("id_variedad") Long varietyId,
        @JsonProperty("fecha_elaboracion") LocalDate productionDate,
        @JsonProperty("stock_total") Integer totalStock,
        @JsonProperty("stock_disponible") Integer availableStock,
        @JsonProperty("costo_unitario_actual") BigDecimal currentUnitCost,
        @JsonProperty("valor_stock_actual") BigDecimal currentStockValue
) {}
