package com.bienCriollas.stock.statistics.interfaces;

import java.time.LocalDate;

import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;

public interface IStatisticsService {

    StatisticsSummaryDTO getSummary(LocalDate start, LocalDate end);
}
