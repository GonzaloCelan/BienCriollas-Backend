package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;

public record EmployeeWorkDayHistoryResponseDTO(
        EmployeeReferenceDTO employee,
        LocalDate from,
        LocalDate to,
        Long totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount,
        Page<EmployeeWorkDayResponseDTO> workDays
) {}
