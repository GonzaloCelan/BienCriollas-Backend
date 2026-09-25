package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;

public record EmployeeDashboardMetricDTO(
        Long workedMinutes,
        BigDecimal workedHours,
        BigDecimal amount,
        Integer employeesWorked
) {}
