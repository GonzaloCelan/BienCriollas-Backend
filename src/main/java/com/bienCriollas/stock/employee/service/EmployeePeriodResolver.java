package com.bienCriollas.stock.employee.service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.employee.enums.WorkDayPeriod;
import com.bienCriollas.stock.employee.exception.InvalidWorkDayException;

@Component
public class EmployeePeriodResolver {

    public static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    public DateRange resolveHistory(
            LocalDate exactDate, LocalDate from, LocalDate to, WorkDayPeriod period) {
        if (exactDate != null) {
            if (from != null || to != null || period != null) {
                throw new InvalidWorkDayException(
                        "El filtro date no puede combinarse con from, to o period.");
            }
            return new DateRange(exactDate, exactDate);
        }
        if (period == null) {
            if (from == null && to == null) {
                return new DateRange(null, null);
            }
            return custom(from, to);
        }
        LocalDate today = LocalDate.now(ARGENTINA_ZONE);
        return switch (period) {
            case TODAY -> new DateRange(today, today);
            case WEEK -> week(today);
            case MONTH -> month(today.getYear(), today.getMonthValue());
            case CUSTOM -> custom(from, to);
        };
    }

    public DateRange week(LocalDate date) {
        LocalDate anchor = date == null ? LocalDate.now(ARGENTINA_ZONE) : date;
        LocalDate start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new DateRange(start, start.plusDays(6));
    }

    public DateRange month(int year, int month) {
        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            return new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        } catch (DateTimeException exception) {
            throw new InvalidWorkDayException("El año o mes solicitado es inválido.");
        }
    }

    public DateRange custom(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidWorkDayException(
                    "Los filtros from y to deben enviarse juntos.");
        }
        if (from.isAfter(to)) {
            throw new InvalidWorkDayException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
        return new DateRange(from, to);
    }

    public record DateRange(LocalDate from, LocalDate to) {}
}
