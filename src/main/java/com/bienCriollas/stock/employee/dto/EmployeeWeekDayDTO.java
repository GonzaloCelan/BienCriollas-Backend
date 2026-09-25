package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeWeekDayDTO(
        LocalDate date,
        Long workDayId,
        Integer shiftCount,
        Integer totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount
) {}
