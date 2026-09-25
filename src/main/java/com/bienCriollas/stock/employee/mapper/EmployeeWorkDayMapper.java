package com.bienCriollas.stock.employee.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.employee.dto.*;
import com.bienCriollas.stock.employee.entity.EmployeeWorkDay;
import com.bienCriollas.stock.employee.entity.EmployeeWorkShift;
import com.bienCriollas.stock.employee.service.EmployeeWorkDayCalculator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmployeeWorkDayMapper {

    private final EmployeeWorkDayCalculator calculator;

    public EmployeeWorkDayResponseDTO toResponse(EmployeeWorkDay workDay) {
        List<WorkShiftResponseDTO> shifts = workDay.getShifts().stream()
                .map(this::toShiftResponse)
                .toList();
        return new EmployeeWorkDayResponseDTO(
                workDay.getId(),
                new EmployeeReferenceDTO(
                        workDay.getEmployee().getId(), workDay.getEmployee().getName()),
                workDay.getWorkDate(),
                workDay.getHourlyRateSnapshot(),
                workDay.getTotalWorkedMinutes(),
                calculator.hours(workDay.getTotalWorkedMinutes()),
                workDay.getTotalAmount(),
                shifts.size(),
                workDay.getNotes(),
                shifts,
                workDay.getCreatedAt(),
                workDay.getUpdatedAt());
    }

    public WorkShiftResponseDTO toShiftResponse(EmployeeWorkShift shift) {
        return new WorkShiftResponseDTO(
                shift.getId(), shift.getStartTime(), shift.getEndTime(),
                shift.getBreakMinutes(), shift.getWorkedMinutes(),
                calculator.hours(shift.getWorkedMinutes()), shift.getSortOrder());
    }

    public WorkShiftRequestDTO toShiftRequest(EmployeeWorkShift shift) {
        return new WorkShiftRequestDTO(
                shift.getStartTime(), shift.getEndTime(), shift.getBreakMinutes());
    }
}
