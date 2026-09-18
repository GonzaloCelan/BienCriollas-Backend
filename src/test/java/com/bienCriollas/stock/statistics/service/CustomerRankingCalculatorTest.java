package com.bienCriollas.stock.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bienCriollas.stock.statistics.dto.CustomerRankingResponseDTO;
import com.bienCriollas.stock.statistics.dto.PeriodDTO;
import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.bienCriollas.stock.statistics.enums.CustomerRankingOrder;
import com.bienCriollas.stock.statistics.repository.StatisticsRepository.OrderCustomerSale;

class CustomerRankingCalculatorTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);
    private static final PeriodDTO PERIOD = new PeriodDTO(AnalysisPeriod.MES, DATE.withDayOfMonth(1), DATE.withDayOfMonth(30));
    private final CustomerRankingCalculator calculator = new CustomerRankingCalculator();

    @Test
    void groupsCaseWhitespaceAndAccentsAndPresentsTheLatestVariantWithoutChangingSources() {
        var sales = List.of(
                sale(4, "  LUCIA   FERNANDEZ ", "10.10", 12),
                sale(7, "Lucía Fernández", "20.20", 24),
                sale(2, "lucia fernandez", "30.30", 6),
                sale(1, "\u00a0Lucía\u2003Fernández\u00a0", "40.40", 12),
                sale(3, "Luci\u0301a Ferna\u0301ndez", "50.50", 12));

        var result = calculate(CustomerRankingOrder.IMPORTE, 5, sales);

        assertThat(result.totalCustomers()).isEqualTo(1);
        var customer = result.customers().get(0);
        assertThat(customer.customer()).isEqualTo("Lucía Fernández");
        assertThat(customer.orders()).isEqualTo(5);
        assertThat(customer.totalSales()).isEqualByComparingTo("151.50");
        assertThat(customer.averageTicket()).isEqualByComparingTo("30.30");
        assertThat(customer.totalUnits()).isEqualTo(66);
        assertThat(sales.get(0).customer()).isEqualTo("  LUCIA   FERNANDEZ ");
        assertThat(result.topPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    void latestPresentationUsesCommercialDateBeforeOrderId() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 5, List.of(
                new OrderCustomerSale(99, "JOSE PEREZ", DATE.minusDays(1), BigDecimal.TEN, 12),
                sale(1, "José Pérez", "10", 12)));
        assertThat(result.customers().get(0).customer()).isEqualTo("José Pérez");
    }

    @Test
    void excludesAllKindsOfAnonymousNamesButIncludesTheirSalesInTheDenominator() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 5, List.of(
                sale(1, null, "10", 12), sale(2, "", "10", 12),
                sale(3, " \t\n ", "10", 12), sale(4, "\u00a0\u2003", "10", 12),
                sale(5, "Juan Pérez", "10", 12)));
        assertThat(result.totalCustomers()).isEqualTo(1);
        assertThat(result.particularSales()).isEqualByComparingTo("50.00");
        assertThat(result.topSales()).isEqualByComparingTo("10.00");
        assertThat(result.topPercentage()).isEqualByComparingTo("20.00");
        assertThat(result.customers().get(0).totalUnits()).isEqualTo(12);
    }

    @Test
    void amountOrderBreaksTiesByOrdersThenSpanishAlphabetAndAssignsPositions() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 5, List.of(
                sale(1, "Zoe", "100", 1), sale(2, "Álvaro", "100", 1),
                sale(3, "Camila", "50", 1), sale(4, "CAMILA", "50", 1),
                sale(5, "Diego", "200", 1)));
        assertThat(result.customers()).extracting(item -> item.customer())
                .containsExactly("Diego", "Camila", "Álvaro", "Zoe");
        assertThat(result.customers()).extracting(item -> item.position()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void orderCountBreaksTiesByAmountThenName() {
        var sales = List.of(sale(1, "Diego", "1000", 1), sale(2, "Camila", "5", 1),
                sale(3, "Camila", "5", 1), sale(4, "Ana", "10", 1),
                sale(5, "Ana", "10", 1), sale(6, "Berta", "10", 1), sale(7, "Berta", "10", 1));
        var result = calculate(CustomerRankingOrder.PEDIDOS, 5, sales);
        assertThat(result.customers()).extracting(item -> item.customer())
                .containsExactly("Ana", "Berta", "Camila", "Diego");
    }

    @Test
    void limitOnlyReducesEntriesAndTopTotalsAndPreservesTheFullCustomerCount() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 1, List.of(
                sale(1, "Ana", "2", 1), sale(2, "Berta", "1", 1)));
        assertThat(result.totalCustomers()).isEqualTo(2);
        assertThat(result.customers()).hasSize(1);
        assertThat(result.topSales()).isEqualByComparingTo("2.00");
        assertThat(result.particularSales()).isEqualByComparingTo("3.00");
        assertThat(result.topPercentage()).isEqualByComparingTo("66.67");
    }

    @Test
    void roundsTheAverageOnceUsingHalfUpAndKeepsMoneyExact() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 5, List.of(
                sale(1, "Ana", "0.01", 1), sale(2, "Ana", "0.02", 1)));
        assertThat(result.customers().get(0).totalSales()).isEqualTo(new BigDecimal("0.03"));
        assertThat(result.customers().get(0).averageTicket()).isEqualTo(new BigDecimal("0.02"));
    }

    @Test
    void zeroSalesAvoidDivisionByZeroAndKeepTheNamedCustomer() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 5, List.of(sale(1, "Ana", "0", 0)));
        assertThat(result.totalCustomers()).isEqualTo(1);
        assertThat(result.topPercentage()).isEqualByComparingTo("0.00");
        assertThat(result.customers().get(0).averageTicket()).isEqualByComparingTo("0.00");
    }

    @Test
    void emptyPeriodHasNoCustomersAndZeroTotals() {
        var result = calculate(CustomerRankingOrder.IMPORTE, 5, List.of());
        assertThat(result.customers()).isEmpty();
        assertThat(result.totalCustomers()).isZero();
        assertThat(result.particularSales()).isEqualByComparingTo("0");
        assertThat(result.topSales()).isEqualByComparingTo("0");
        assertThat(result.topPercentage()).isEqualByComparingTo("0");
    }

    private CustomerRankingResponseDTO calculate(CustomerRankingOrder order, int limit, List<OrderCustomerSale> sales) {
        return calculator.calculate(PERIOD, order, limit, sales);
    }

    private OrderCustomerSale sale(long id, String name, String amount, long units) {
        return new OrderCustomerSale(id, name, DATE, new BigDecimal(amount), units);
    }
}
