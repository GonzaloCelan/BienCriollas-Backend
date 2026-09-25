package com.bienCriollas.stock.employee.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record EmployeeWorkDayCreateRequestDTO(
        @NotNull @Positive Long employeeId,
        @NotNull LocalDate workDate,
        @Size(max = 500) String notes,
        @NotEmpty List<@Valid WorkShiftRequestDTO> shifts
) {}
