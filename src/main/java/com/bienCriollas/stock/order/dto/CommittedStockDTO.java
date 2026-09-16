package com.bienCriollas.stock.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommittedStockDTO(
        @JsonProperty("variedadId") Long varietyId,
        @JsonProperty("variedad") String variety,
        @JsonProperty("stockFisico") long physicalStock,
        @JsonProperty("stockComprometido") long committedStock,
        @JsonProperty("stockDisponible") long availableStock) {
}
