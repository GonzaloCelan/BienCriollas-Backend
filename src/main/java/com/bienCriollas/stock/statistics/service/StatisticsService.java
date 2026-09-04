package com.bienCriollas.stock.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;
import com.bienCriollas.stock.statistics.exception.InvalidStatisticsRangeException;
import com.bienCriollas.stock.statistics.interfaces.IStatisticsService;
import com.bienCriollas.stock.statistics.repository.StatisticsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService implements IStatisticsService {

    private final StatisticsRepository statisticsRepository;

    @Override
    @Transactional(readOnly = true)
    public StatisticsSummaryDTO getSummary(LocalDate start, LocalDate end) {
        validateRange(start, end);

        Integer deliveredOrders = statisticsRepository.countDeliveredOrders(start, end);
        Integer soldEmpanadas = statisticsRepository.countSoldEmpanadas(start, end);
        BigDecimal totalSales = statisticsRepository.sumTotalSales(start, end);

        BigDecimal averageTicket = calculateAverageTicket(totalSales, deliveredOrders);

        List<StatisticsSummaryDTO.VarietyRankingDTO> varietyRanking =
                statisticsRepository.getVarietyRanking(start, end);

        StatisticsSummaryDTO.BestSellingVarietyDTO bestSellingVariety =
                varietyRanking.isEmpty()
                        ? null
                        : new StatisticsSummaryDTO.BestSellingVarietyDTO(
                                varietyRanking.get(0).varietyId(),
                                varietyRanking.get(0).name(),
                                varietyRanking.get(0).unitsSold()
                        );

        List<StatisticsSummaryDTO.SalesByWeekdayDTO> salesByWeekday =
                statisticsRepository.getSalesByWeekday(start, end);

        List<StatisticsSummaryDTO.SaleTypeSummaryDTO> saleTypes =
                statisticsRepository.getSaleTypes(start, end);

        List<StatisticsSummaryDTO.PaymentMethodSummaryDTO> paymentMethods =
                statisticsRepository.getPaymentMethods(start, end);

        List<StatisticsSummaryDTO.WasteByVarietyDTO> wasteByVariety =
                statisticsRepository.getWasteByVariety(start, end);

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

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new InvalidStatisticsRangeException("Las fechas desde y hasta son obligatorias");
        }

        if (!start.isBefore(end)) {
            throw new InvalidStatisticsRangeException("La fecha desde debe ser menor que la fecha hasta");
        }
    }

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
