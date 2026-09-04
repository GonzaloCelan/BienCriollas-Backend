package com.bienCriollas.stock.statistics.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AIAnalysisResponse(
        @JsonProperty("titulo") String title,
        @JsonProperty("analisis") String analysis,
        @JsonProperty("recomendaciones") List<String> recommendations
) {}
