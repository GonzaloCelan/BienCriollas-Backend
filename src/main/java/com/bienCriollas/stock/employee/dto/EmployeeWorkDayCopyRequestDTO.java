package com.bienCriollas.stock.employee.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record EmployeeWorkDayCopyRequestDTO(@NotNull LocalDate targetDate) {}
