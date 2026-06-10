package com.bienCriollas.stock.Interface;

import java.time.LocalDate;

import com.bienCriollas.stock.Dto.estadistica.EstadisticaResumenDTO;

public interface IEstadisticaService {

    EstadisticaResumenDTO obtenerResumen(LocalDate desde, LocalDate hasta);
}