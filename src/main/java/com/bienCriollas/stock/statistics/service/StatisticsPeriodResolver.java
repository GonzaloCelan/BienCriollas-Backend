package com.bienCriollas.stock.statistics.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.statistics.enums.AnalysisPeriod;
import com.bienCriollas.stock.statistics.exception.InvalidStatisticsRangeException;

@Component
public class StatisticsPeriodResolver {

    public DateRange resolve(AnalysisPeriod period, LocalDate date, YearMonth month, Integer year) {
        if (period == null) {
            throw new InvalidStatisticsRangeException("El periodo es obligatorio");
        }
        try {
            return switch (period) {
                case DIA -> {
                    requireDate(date);
                    yield new DateRange(date, date.plusDays(1));
                }
                case ULTIMOS_7_DIAS -> {
                    requireDate(date);
                    yield new DateRange(date.minusDays(6), date.plusDays(1));
                }
                case MES -> {
                    if (month == null) {
                        throw new InvalidStatisticsRangeException("El mes es obligatorio para el periodo MES");
                    }
                    yield new DateRange(month.atDay(1), month.plusMonths(1).atDay(1));
                }
                case ANIO -> {
                    if (year == null) {
                        throw new InvalidStatisticsRangeException("El anio es obligatorio para el periodo ANIO");
                    }
                    // Both bounds must fit MySQL DATE; the upper bound is exclusive.
                    if (year < 1000 || year > 9998) {
                        throw new InvalidStatisticsRangeException("El anio debe estar entre 1000 y 9998");
                    }
                    yield new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
                }
            };
        } catch (DateTimeException exception) {
            throw new InvalidStatisticsRangeException("El periodo solicitado excede el rango de fechas permitido");
        }
    }

    public DateRange resolveSummary(AnalysisPeriod period, LocalDate date, YearMonth month,
            Integer year, LocalDate start, LocalDate end) {
        if (period == null) {
            if (date != null || month != null || year != null) {
                throw new InvalidStatisticsRangeException("El periodo es obligatorio al usar fecha, mes o anio");
            }
            return new DateRange(start, end);
        }
        if (start != null || end != null) {
            throw new InvalidStatisticsRangeException("Usar periodo o desde/hasta, sin combinar ambos filtros");
        }
        return resolve(period, date, month, year);
    }

    private void requireDate(LocalDate date) {
        if (date == null) {
            throw new InvalidStatisticsRangeException("La fecha es obligatoria para DIA y ULTIMOS_7_DIAS");
        }
    }

    /** El módulo de estadísticas usa siempre inicio inclusivo y fin exclusivo. */
    public record DateRange(LocalDate from, LocalDate until) {
        public DateRange {
            if (from == null || until == null) {
                throw new InvalidStatisticsRangeException("Las fechas desde y hasta son obligatorias");
            }
            if (!from.isBefore(until)) {
                throw new InvalidStatisticsRangeException("La fecha desde debe ser menor que la fecha hasta");
            }
        }
    }
}
