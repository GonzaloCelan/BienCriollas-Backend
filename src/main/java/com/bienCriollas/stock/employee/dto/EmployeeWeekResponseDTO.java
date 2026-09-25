package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmployeeWeekResponseDTO(
        LocalDate from,
        LocalDate to,
        List<EmployeeWeekRowDTO> employees,
        Long totalWorkedMinutes,
        BigDecimal totalWorkedHours,
        BigDecimal totalAmount
) {}
