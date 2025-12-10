package com.bienCriollas.stock.Dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoRequestDTO(
	    Long idEmpleado,
	    LocalDate fecha,
	    LocalTime horaInicio,
	    LocalTime horaFin
	) {}