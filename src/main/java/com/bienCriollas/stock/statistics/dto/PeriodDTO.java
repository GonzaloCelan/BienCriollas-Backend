package com.bienCriollas.stock.statistics.dto;

import java.time.LocalDate;
import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PeriodDTO(
        @JsonProperty("tipo") AnalysisPeriod type,
        @JsonProperty("desde") LocalDate from,
        @JsonProperty("hasta") LocalDate through
) {}
