package com.bienCriollas.stock.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.order.util.CustomerNameNormalizer;
import com.bienCriollas.stock.statistics.dto.CustomerRankingResponseDTO;
import com.bienCriollas.stock.statistics.dto.CustomerRankingResponseDTO.CustomerRankingItemDTO;
import com.bienCriollas.stock.statistics.dto.PeriodDTO;
import com.bienCriollas.stock.statistics.enums.CustomerRankingOrder;
import com.bienCriollas.stock.statistics.repository.StatisticsRepository.OrderCustomerSale;

@Component
public class CustomerRankingCalculator {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    public CustomerRankingResponseDTO calculate(PeriodDTO period, CustomerRankingOrder order,
            int limit, List<OrderCustomerSale> sales) {
        Map<String, CustomerTotals> grouped = new HashMap<>();
        BigDecimal particularSales = ZERO;
        for (OrderCustomerSale sale : sales) {
            BigDecimal amount = sale.totalSales() == null ? ZERO : sale.totalSales();
            // Anonymous sales belong to the period's sales, but not to a named customer.
            particularSales = particularSales.add(amount);
            String name = cleanName(sale.customer());
            if (name.isEmpty()) {
                continue;
            }
            String key = ACCENTS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD))
                    .replaceAll("").toLowerCase(Locale.ROOT);
            grouped.computeIfAbsent(key, ignored -> new CustomerTotals(key)).add(sale, name, amount);
        }

        // Collator is mutable: keep it local so simultaneous requests cannot interfere.
        Collator alphabet = Collator.getInstance(Locale.forLanguageTag("es-AR"));
        alphabet.setStrength(Collator.PRIMARY);
        Comparator<CustomerTotals> bySales = Comparator.comparing(
                (CustomerTotals customer) -> customer.totalSales).reversed();
        Comparator<CustomerTotals> byOrders = Comparator.comparingLong(
                (CustomerTotals customer) -> customer.orders).reversed();
        Comparator<CustomerTotals> ordering = order == CustomerRankingOrder.PEDIDOS
                ? byOrders.thenComparing(bySales) : bySales.thenComparing(byOrders);
        ordering = ordering.thenComparing(customer -> customer.name, alphabet)
                .thenComparing(customer -> customer.key);

        List<CustomerTotals> top = grouped.values().stream().sorted(ordering).limit(limit).toList();
        List<CustomerRankingItemDTO> customers = new ArrayList<>(top.size());
        BigDecimal topSales = ZERO;
        for (CustomerTotals customer : top) {
            topSales = topSales.add(customer.totalSales);
            customers.add(new CustomerRankingItemDTO(customers.size() + 1, customer.name,
                    customer.orders, customer.totalSales.setScale(2, RoundingMode.HALF_UP),
                    customer.totalSales.divide(BigDecimal.valueOf(customer.orders), 2, RoundingMode.HALF_UP),
                    customer.units));
        }
        BigDecimal percentage = particularSales.signum() == 0 ? ZERO
                : topSales.multiply(BigDecimal.valueOf(100)).divide(particularSales, 2, RoundingMode.HALF_UP);
        return new CustomerRankingResponseDTO(period, order, grouped.size(),
                particularSales.setScale(2, RoundingMode.HALF_UP),
                topSales.setScale(2, RoundingMode.HALF_UP), percentage, List.copyOf(customers));
    }

    private String cleanName(String name) {
        return name == null ? "" : WHITESPACE.matcher(name).replaceAll(" ").trim();
    }

    private static final class CustomerTotals {
        private final String key;
        private String name;
        private OrderCustomerSale latest;
        private long orders;
        private long units;
        private BigDecimal totalSales = ZERO;

        private CustomerTotals(String key) {
            this.key = key;
        }

        private void add(OrderCustomerSale sale, String cleanName, BigDecimal amount) {
            orders++;
            units += sale.totalUnits();
            totalSales = totalSales.add(amount);
            if (latest == null || sale.commercialDate().isAfter(latest.commercialDate())
                    || (sale.commercialDate().equals(latest.commercialDate()) && sale.orderId() > latest.orderId())) {
                latest = sale;
                name = CustomerNameNormalizer.normalize(cleanName);
            }
        }
    }
}
