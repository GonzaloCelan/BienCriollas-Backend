package com.bienCriollas.stock.egreso.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.egreso.dto.EgresoResponseDTO;
import com.bienCriollas.stock.egreso.dto.EgresoTipoDTO;
import com.bienCriollas.stock.egreso.dto.EgresoTotalPorTipoDTO;
import com.bienCriollas.stock.egreso.dto.EgresosPorcentajeDTO;
import com.bienCriollas.stock.egreso.enums.TipoEgreso;
import com.bienCriollas.stock.egreso.entity.Egreso;

public interface IEgresoService {

    Egreso registrarEgreso(EgresoTipoDTO request);

    EgresoResponseDTO calcularEgresoAcumulado();

    List<EgresosPorcentajeDTO> obtenerKpisMesActualVsAnterior();

    List<EgresoTotalPorTipoDTO> obtenerTotalesPorTipo(int anio, int mes);

    List<Egreso> obtenerEgresosDeHoy();

    Page<Egreso> listarHistorial(
            int anio,
            int mes,
            TipoEgreso tipoEgreso,
            Pageable pageable
    );

    Page<Egreso> listarHistorial(
            int anio,
            int mes,
            Pageable pageable
    );

    Page<Egreso> listarPorTipoEgreso(
            TipoEgreso tipo,
            Pageable pageable
    );

    List<Egreso> obtenerUltimosMovimientos();
}