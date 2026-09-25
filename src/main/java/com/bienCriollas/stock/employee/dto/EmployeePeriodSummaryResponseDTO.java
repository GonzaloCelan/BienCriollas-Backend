package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmployeePeriodSummaryResponseDTO(
        LocalDate from,
        LocalDate to,
        List<EmployeePeriodSummaryItemDTO> employees,
        Long totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount
) {}
