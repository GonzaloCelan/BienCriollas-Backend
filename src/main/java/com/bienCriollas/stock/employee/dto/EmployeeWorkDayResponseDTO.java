package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeWorkDayResponseDTO(
        Long id,
        EmployeeReferenceDTO employee,
        LocalDate workDate,
        BigDecimal hourlyRateSnapshot,
        Integer totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount,
        Integer shiftCount,
        String notes,
        List<WorkShiftResponseDTO> shifts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
