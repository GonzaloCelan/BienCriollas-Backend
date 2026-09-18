package com.bienCriollas.stock.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO;
import com.bienCriollas.stock.statistics.dto.PeriodDTO;
import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.bienCriollas.stock.statistics.enums.ShiftType;
import com.bienCriollas.stock.statistics.repository.StatisticsRepository.OrderTimeSale;

class PeakHourCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);
    private final PeakHourCalculator calculator = new PeakHourCalculator();

    @ParameterizedTest
    @CsvSource({
            "11:30:00,11:30,MEDIODIA", "11:59:59,11:30,MEDIODIA",
            "12:00:00,12:00,MEDIODIA", "12:29:59,12:00,MEDIODIA",
            "12:30:00,12:30,MEDIODIA", "14:29:59,14:00,MEDIODIA",
            "20:30:00,20:30,NOCHE", "21:59:59,21:30,NOCHE",
            "22:00:00,22:00,NOCHE", "23:29:59,23:00,NOCHE"
    })
    void assignsShiftAndHalfHourUsingInclusiveStartAndExclusiveEnd(
            LocalTime createdAt, LocalTime slotStart, ShiftType shift) {
        PeakHourResponseDTO response = calculate(List.of(sale(createdAt, "18000")));

        assertThat(response.peakSlot().start()).isEqualTo(slotStart);
        assertThat(response.peakSlot().end()).isEqualTo(slotStart.plusMinutes(30));
        assertThat(response.peakSlot().shift()).isEqualTo(shift);
        assertThat(response.peakSlot().orders()).isEqualTo(1);
        assertThat(response.peakSlot().percentage()).isEqualByComparingTo("100");
        assertThat(response.outsideShiftOrders()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"00:00:00", "11:29:59", "14:30:00", "20:29:59", "23:30:00", "23:59:59"})
    void outsideShiftOrdersDoNotCreateAPeakOrChangeTheTwelveBars(LocalTime createdAt) {
        PeakHourResponseDTO response = calculate(List.of(sale(createdAt, "9000")));

        assertThat(response.peakSlot()).isNull();
        assertThat(response.outsideShiftOrders()).isEqualTo(1);
        assertThat(response.totalOrders()).isEqualTo(1);
        assertThat(response.outsideShiftSales()).isEqualByComparingTo("9000");
        assertThat(response.shifts().midday().slots()).hasSize(6).allSatisfy(slot -> {
            assertThat(slot.orders()).isZero();
            assertThat(slot.sales()).isZero();
        });
        assertThat(response.shifts().night().slots()).hasSize(6).allSatisfy(slot -> {
            assertThat(slot.orders()).isZero();
            assertThat(slot.sales()).isZero();
        });
    }

    @Test
    void peakUsesOrderCountBeforeSalesAndSalesBeforeChronologicalOrder() {
        PeakHourResponseDTO response = calculate(List.of(
                sale(LocalTime.of(11, 30), "5000"),
                sale(LocalTime.of(12, 0), "10"), sale(LocalTime.of(12, 15), "10"),
                sale(LocalTime.of(21, 30), "20"), sale(LocalTime.of(21, 45), "20")));

        assertThat(response.peakSlot().start()).isEqualTo(LocalTime.of(21, 30));
        assertThat(response.peakSlot().orders()).isEqualTo(2);
        assertThat(response.peakSlot().sales()).isEqualByComparingTo("40");
    }

    @Test
    void equalCountsAndAmountsSelectTheEarlierSlotRegardlessOfInputOrder() {
        PeakHourResponseDTO response = calculate(List.of(
                sale(LocalTime.of(21, 30), "20"), sale(LocalTime.of(21, 45), "20"),
                sale(LocalTime.of(12, 0), "20"), sale(LocalTime.of(12, 15), "20")));

        assertThat(response.peakSlot().start()).isEqualTo(LocalTime.of(12, 0));
        assertThat(response.peakSlot().shift()).isEqualTo(ShiftType.MEDIODIA);
    }

    @Test
    void preservesDecimalAmountsAndCalculatesPercentagesOverAllAnalyzedOrders() {
        PeakHourResponseDTO response = calculate(List.of(
                sale(LocalTime.of(21, 30), "0.10"),
                sale(LocalTime.of(21, 45), "0.20"),
                sale(LocalTime.of(5, 0), "0.30")));

        assertThat(response.totalSales()).isEqualByComparingTo("0.60");
        assertThat(response.peakSlot().sales()).isEqualByComparingTo("0.30");
        assertThat(response.peakSlot().percentage()).isEqualByComparingTo("66.67");
        assertThat(response.shifts().night().slots().get(2).percentage()).isEqualByComparingTo("66.67");
        assertThat(response.outsideShiftSales()).isEqualByComparingTo("0.30");
    }

    private PeakHourResponseDTO calculate(List<OrderTimeSale> sales) {
        return calculator.calculate(new PeriodDTO(AnalysisPeriod.DIA, DATE, DATE), sales);
    }

    private OrderTimeSale sale(LocalTime time, String amount) {
        return new OrderTimeSale(LocalDateTime.of(DATE, time), new BigDecimal(amount));
    }
}
