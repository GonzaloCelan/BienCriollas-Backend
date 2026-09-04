package com.bienCriollas.stock.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AIAnalysisRequest(
        @JsonProperty("tipoAnalisis") String analysisType,
        @JsonProperty("fecha") String date
) {}
