package com.bienCriollas.stock.employee.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.employee.dto.WorkShiftRequestDTO;
import com.bienCriollas.stock.employee.entity.EmployeeWorkShift;
import com.bienCriollas.stock.employee.exception.*;

@Component
public class EmployeeWorkDayCalculator {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    public Calculation calculate(
            List<WorkShiftRequestDTO> requestedShifts, BigDecimal hourlyRateSnapshot) {
        if (requestedShifts == null || requestedShifts.isEmpty()) {
            throw new InvalidWorkDayException("La jornada debe tener al menos un turno.");
        }
        if (hourlyRateSnapshot == null || hourlyRateSnapshot.signum() <= 0) {
            throw new InvalidWorkDayException("El valor hora de la jornada debe ser mayor a cero.");
        }

        List<ValidatedShift> validated = requestedShifts.stream()
                .map(this::validateShift)
                .sorted(Comparator.comparing((ValidatedShift shift) -> shift.request().startTime())
                        .thenComparing(shift -> shift.request().endTime()))
                .toList();
        for (int index = 1; index < validated.size(); index++) {
            if (validated.get(index).request().startTime()
                    .isBefore(validated.get(index - 1).request().endTime())) {
                throw new OverlappingWorkShiftException();
            }
        }

        int totalMinutes = 0;
        List<EmployeeWorkShift> shifts = new ArrayList<>();
        for (int index = 0; index < validated.size(); index++) {
            ValidatedShift item = validated.get(index);
            totalMinutes = Math.addExact(totalMinutes, item.workedMinutes());
            shifts.add(EmployeeWorkShift.builder()
                    .startTime(item.request().startTime())
                    .endTime(item.request().endTime())
                    .breakMinutes(item.breakMinutes())
                    .workedMinutes(item.workedMinutes())
                    .sortOrder(index + 1)
                    .build());
        }

        BigDecimal totalAmount = hourlyRateSnapshot
                .multiply(BigDecimal.valueOf(totalMinutes))
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
        return new Calculation(List.copyOf(shifts), totalMinutes, totalAmount);
    }

    public BigDecimal hours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    private ValidatedShift validateShift(WorkShiftRequestDTO shift) {
        if (shift == null || shift.startTime() == null || shift.endTime() == null) {
            throw new InvalidWorkShiftException(
                    "La hora de inicio y la hora de fin son obligatorias.");
        }
        if (shift.startTime().getSecond() != 0 || shift.startTime().getNano() != 0
                || shift.endTime().getSecond() != 0 || shift.endTime().getNano() != 0) {
            throw new InvalidWorkShiftException(
                    "Los horarios deben informarse con precisión de minutos.");
        }
        if (!shift.endTime().isAfter(shift.startTime())) {
            throw new InvalidWorkShiftException(
                    "La hora de fin debe ser posterior a la hora de inicio.");
        }
        int breakMinutes = shift.breakMinutes() == null ? 0 : shift.breakMinutes();
        if (breakMinutes < 0) {
            throw new InvalidWorkShiftException(
                    "Los minutos de descanso no pueden ser negativos.");
        }
        long grossMinutes = Duration.between(shift.startTime(), shift.endTime()).toMinutes();
        if (breakMinutes >= grossMinutes) {
            throw new InvalidWorkShiftException(
                    "Los minutos de descanso deben ser menores a la duración del turno.");
        }
        return new ValidatedShift(shift, breakMinutes, Math.toIntExact(grossMinutes) - breakMinutes);
    }

    private record ValidatedShift(
            WorkShiftRequestDTO request, int breakMinutes, int workedMinutes) {}

    public record Calculation(
            List<EmployeeWorkShift> shifts, int totalWorkedMinutes, BigDecimal totalAmount) {}
}
