package com.bienCriollas.stock.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderReconciliationResultDTO(
        @JsonProperty("idLote") String batchId,
        @JsonProperty("anio") int year,
        @JsonProperty("mes") int month,
        @JsonProperty("cantidadActualizada") int updatedCount,
        @JsonProperty("ingresoIncorporado") BigDecimal incorporatedIncome,
        @JsonProperty("idsPedidos") List<Long> orderIds,
        @JsonProperty("realizadoPor") String performedBy,
        @JsonProperty("realizadoEn") OffsetDateTime performedAt) {
}
