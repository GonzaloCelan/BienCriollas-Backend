package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;

public record EmployeePeriodSummaryItemDTO(
        Long employeeId,
        String employeeName,
        Long workedMinutes,
        BigDecimal workedHours,
        BigDecimal amount
) {}
