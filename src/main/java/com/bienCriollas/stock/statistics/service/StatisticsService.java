package com.bienCriollas.stock.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;
import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO;
import com.bienCriollas.stock.statistics.dto.PeriodDTO;
import com.bienCriollas.stock.statistics.dto.CustomerRankingResponseDTO;
import com.bienCriollas.stock.statistics.enums.CustomerRankingOrder;
import com.bienCriollas.stock.statistics.exception.InvalidStatisticsRangeException;
import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.bienCriollas.stock.statistics.interfaces.IStatisticsService;
import com.bienCriollas.stock.statistics.repository.StatisticsRepository;
import com.bienCriollas.stock.statistics.service.StatisticsPeriodResolver.DateRange;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService implements IStatisticsService {

    private final StatisticsRepository statisticsRepository;
    private final StatisticsPeriodResolver periodResolver;
    private final PeakHourCalculator peakHourCalculator;
    private final CustomerRankingCalculator customerRankingCalculator;

    @Override
    @Transactional(readOnly = true)
    public CustomerRankingResponseDTO getCustomerRanking(AnalysisPeriod period, LocalDate date,
            YearMonth month, Integer year, CustomerRankingOrder order, int limit) {
        if (limit < 1 || limit > 100) {
            throw new InvalidStatisticsRangeException("limit debe estar entre 1 y 100.");
        }
        if (order == null) {
            throw new InvalidStatisticsRangeException("orden debe ser IMPORTE o PEDIDOS.");
        }
        DateRange range = periodResolver.resolve(period, date, month, year);
        var sales = statisticsRepository.getDeliveredParticularCustomerSales(range.from(), range.until());
        return customerRankingCalculator.calculate(
                new PeriodDTO(period, range.from(), range.until().minusDays(1)), order, limit, sales);
    }

    @Override
    @Transactional(readOnly = true)
    public PeakHourResponseDTO getPeakHour(AnalysisPeriod period, LocalDate date, YearMonth month, Integer year) {
        DateRange range = periodResolver.resolve(period, date, month, year);
        var orders = statisticsRepository.getDeliveredOrderTimes(
                range.from().atStartOfDay(), range.until().atStartOfDay());
        return peakHourCalculator.calculate(
                new PeriodDTO(period, range.from(), range.until().minusDays(1)), orders);
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsSummaryDTO getSummary(AnalysisPeriod period, LocalDate date, YearMonth month,
            Integer year, LocalDate start, LocalDate end) {
        DateRange range = periodResolver.resolveSummary(period, date, month, year, start, end);
        return getSummary(range.from(), range.until());
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsSummaryDTO getSummary(LocalDate start, LocalDate end) {
        DateRange range = new DateRange(start, end);

        Integer deliveredOrders = statisticsRepository.countDeliveredOrders(range.from(), range.until());
        Integer soldEmpanadas = statisticsRepository.countSoldEmpanadas(range.from(), range.until());
        BigDecimal totalSales = statisticsRepository.sumTotalSales(range.from(), range.until());

        BigDecimal averageTicket = calculateAverageTicket(totalSales, deliveredOrders);

        List<StatisticsSummaryDTO.VarietyRankingDTO> varietyRanking =
                statisticsRepository.getVarietyRanking(range.from(), range.until());

        StatisticsSummaryDTO.BestSellingVarietyDTO bestSellingVariety =
                varietyRanking.isEmpty()
                        ? null
                        : new StatisticsSummaryDTO.BestSellingVarietyDTO(
                                varietyRanking.get(0).varietyId(),
                                varietyRanking.get(0).name(),
                                varietyRanking.get(0).unitsSold()
                        );

        List<StatisticsSummaryDTO.SalesByWeekdayDTO> salesByWeekday =
                statisticsRepository.getSalesByWeekday(range.from(), range.until());

        List<StatisticsSummaryDTO.SaleTypeSummaryDTO> saleTypes =
                statisticsRepository.getSaleTypes(range.from(), range.until());

        List<StatisticsSummaryDTO.PaymentMethodSummaryDTO> paymentMethods =
                statisticsRepository.getPaymentMethods(range.from(), range.until());

        List<StatisticsSummaryDTO.WasteByVarietyDTO> wasteByVariety =
                statisticsRepository.getWasteByVariety(range.from(), range.until());

        return new StatisticsSummaryDTO(
                deliveredOrders,
                soldEmpanadas,
                averageTicket,
                bestSellingVariety,
                varietyRanking,
                salesByWeekday,
                saleTypes,
                paymentMethods,
                wasteByVariety
        );
    }

	//mertodos privados para validaciones y cálculos

    private BigDecimal calculateAverageTicket(BigDecimal totalSales, Integer deliveredOrders) {
        if (deliveredOrders == null || deliveredOrders == 0) {
            return BigDecimal.ZERO;
        }

        return normalize(totalSales)
                .divide(BigDecimal.valueOf(deliveredOrders), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
