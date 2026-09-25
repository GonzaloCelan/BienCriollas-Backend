package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeResponseDTO(
        Long id,
        String name,
        BigDecimal hourlyRate,
        Boolean active,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
