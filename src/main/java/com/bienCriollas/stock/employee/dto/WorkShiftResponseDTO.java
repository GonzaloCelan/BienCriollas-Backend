package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record WorkShiftResponseDTO(
        Long id,
        LocalTime startTime,
        LocalTime endTime,
        Integer breakMinutes,
        Integer workedMinutes,
        BigDecimal workedHours,
        Integer sortOrder
) {}
