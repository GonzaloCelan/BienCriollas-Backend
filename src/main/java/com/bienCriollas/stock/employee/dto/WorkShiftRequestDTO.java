package com.bienCriollas.stock.employee.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.*;

public record WorkShiftRequestDTO(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @PositiveOrZero Integer breakMinutes
) {}
