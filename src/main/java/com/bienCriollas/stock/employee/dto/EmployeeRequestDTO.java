package com.bienCriollas.stock.employee.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record EmployeeRequestDTO(
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 17, fraction = 2) BigDecimal hourlyRate,
        @Size(max = 500) String notes
) {}
