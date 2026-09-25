package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.util.List;

public record EmployeeWeekRowDTO(
        Long employeeId,
        String employeeName,
        BigDecimal hourlyRate,
        Boolean active,
        List<EmployeeWeekDayDTO> days,
        Long weekWorkedMinutes,
        BigDecimal weekWorkedHours,
        BigDecimal weekAmount
) {}
