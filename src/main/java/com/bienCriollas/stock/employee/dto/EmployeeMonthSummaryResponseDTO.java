package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.util.List;

public record EmployeeMonthSummaryResponseDTO(
        Integer year,
        Integer month,
        List<EmployeePeriodSummaryItemDTO> employees,
        Long totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount
) {}
