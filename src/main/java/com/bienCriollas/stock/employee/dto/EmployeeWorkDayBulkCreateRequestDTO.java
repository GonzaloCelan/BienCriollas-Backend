package com.bienCriollas.stock.employee.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record EmployeeWorkDayBulkCreateRequestDTO(
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> employeeIds,
        @NotNull LocalDate workDate,
        @Size(max = 500) String notes,
        @NotEmpty List<@Valid WorkShiftRequestDTO> shifts
) {}
