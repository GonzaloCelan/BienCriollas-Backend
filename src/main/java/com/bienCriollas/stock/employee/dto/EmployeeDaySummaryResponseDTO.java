package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmployeeDaySummaryResponseDTO(
        LocalDate date,
        Integer employeesWorked,
        Long totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount,
        List<EmployeePeriodSummaryItemDTO> employees
) {}
