package com.bienCriollas.stock.Dto;

import java.time.LocalTime;

public record TurnoEditRequestDTO(
        LocalTime horaInicio,
        LocalTime horaFin
) {}