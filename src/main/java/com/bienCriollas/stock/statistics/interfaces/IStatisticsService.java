package com.bienCriollas.stock.statistics.interfaces;

import java.time.LocalDate;
import java.time.YearMonth;

import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO;
import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;
import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.bienCriollas.stock.statistics.enums.CustomerRankingOrder;
import com.bienCriollas.stock.statistics.dto.CustomerRankingResponseDTO;

public interface IStatisticsService {

    StatisticsSummaryDTO getSummary(LocalDate start, LocalDate end);

    PeakHourResponseDTO getPeakHour(AnalysisPeriod period, LocalDate date, YearMonth month);

    CustomerRankingResponseDTO getCustomerRanking(AnalysisPeriod period, LocalDate date,
            YearMonth month, CustomerRankingOrder order, int limit);
}
