package com.bienCriollas.stock.estadistica.interfaces;

import java.time.LocalDate;

import com.bienCriollas.stock.estadistica.dto.EstadisticaResumenDTO;

public interface IEstadisticaService {

    EstadisticaResumenDTO obtenerResumen(LocalDate desde, LocalDate hasta);
}