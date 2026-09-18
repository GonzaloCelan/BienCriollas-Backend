package com.bienCriollas.stock.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO;
import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO.PeakSlotDTO;
import com.bienCriollas.stock.statistics.dto.PeriodDTO;
import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO.ShiftStatsDTO;
import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO.ShiftsDTO;
import com.bienCriollas.stock.statistics.dto.PeakHourResponseDTO.TimeSlotDTO;
import com.bienCriollas.stock.statistics.enums.ShiftType;
import com.bienCriollas.stock.statistics.repository.StatisticsRepository.OrderTimeSale;

@Component
public class PeakHourCalculator {

    private static final LocalTime MIDDAY_START = LocalTime.of(11, 30);
    private static final LocalTime NIGHT_START = LocalTime.of(20, 30);
    private static final int SLOTS_PER_SHIFT = 6;
    private static final int SLOT_MINUTES = 30;

    public PeakHourResponseDTO calculate(PeriodDTO period, List<OrderTimeSale> orders) {
        List<Slot> slots = new ArrayList<>();
        addShift(slots, MIDDAY_START, ShiftType.MEDIODIA);
        addShift(slots, NIGHT_START, ShiftType.NOCHE);

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal outsideSales = BigDecimal.ZERO;
        long outsideOrders = 0;
        for (OrderTimeSale order : orders) {
            BigDecimal amount = order.totalSales() == null ? BigDecimal.ZERO : order.totalSales();
            totalSales = totalSales.add(amount);
            // createdAt ya está almacenado como hora local argentina, sin conversión a UTC.
            LocalTime time = order.createdAt().toLocalTime();
            Slot slot = slots.stream().filter(candidate -> candidate.contains(time)).findFirst().orElse(null);
            if (slot == null) {
                outsideOrders++;
                outsideSales = outsideSales.add(amount);
            } else {
                slot.orders++;
                slot.sales = slot.sales.add(amount);
            }
        }

        long totalOrders = orders.size();
        Slot peak = null;
        for (Slot slot : slots) {
            if (slot.orders > 0 && (peak == null
                    || slot.orders > peak.orders
                    || (slot.orders == peak.orders && slot.sales.compareTo(peak.sales) > 0))) {
                peak = slot;
            }
        }
        // Las franjas se recorren cronológicamente: un empate completo conserva la más temprana.
        PeakSlotDTO peakSlot = peak == null ? null : new PeakSlotDTO(
                peak.start, peak.end, peak.orders, peak.sales, percentage(peak.orders, totalOrders), peak.shift);

        return new PeakHourResponseDTO(period, totalOrders, totalSales, peakSlot,
                new ShiftsDTO(
                        summarizeShift(slots.subList(0, SLOTS_PER_SHIFT), totalOrders),
                        summarizeShift(slots.subList(SLOTS_PER_SHIFT, slots.size()), totalOrders)),
                outsideOrders, outsideSales);
    }

    private void addShift(List<Slot> slots, LocalTime start, ShiftType shift) {
        for (int index = 0; index < SLOTS_PER_SHIFT; index++) {
            LocalTime slotStart = start.plusMinutes((long) index * SLOT_MINUTES);
            slots.add(new Slot(slotStart, slotStart.plusMinutes(SLOT_MINUTES), shift));
        }
    }

    private ShiftStatsDTO summarizeShift(List<Slot> slots, long totalOrders) {
        long shiftOrders = 0;
        BigDecimal shiftSales = BigDecimal.ZERO;
        List<TimeSlotDTO> distribution = new ArrayList<>();
        for (Slot slot : slots) {
            shiftOrders += slot.orders;
            shiftSales = shiftSales.add(slot.sales);
            distribution.add(new TimeSlotDTO(slot.start, slot.end, slot.orders, slot.sales,
                    percentage(slot.orders, totalOrders)));
        }
        return new ShiftStatsDTO(slots.get(0).start, slots.get(slots.size() - 1).end,
                shiftOrders, shiftSales, List.copyOf(distribution));
    }

    private BigDecimal percentage(long orders, long totalOrders) {
        return totalOrders == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(orders)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
    }

    private static final class Slot {
        private final LocalTime start;
        private final LocalTime end;
        private final ShiftType shift;
        private long orders;
        private BigDecimal sales = BigDecimal.ZERO;

        private Slot(LocalTime start, LocalTime end, ShiftType shift) {
            this.start = start;
            this.end = end;
            this.shift = shift;
        }

        private boolean contains(LocalTime time) {
            return !time.isBefore(start) && time.isBefore(end);
        }
    }
}
