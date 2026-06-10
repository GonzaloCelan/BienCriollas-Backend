package com.bienCriollas.stock.Interface;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.Dto.egreso.EgresoResponseDTO;
import com.bienCriollas.stock.Dto.egreso.EgresoTipoDTO;
import com.bienCriollas.stock.Dto.egreso.EgresoTotalPorTipoDTO;
import com.bienCriollas.stock.Dto.egreso.EgresosPorcentajeDTO;
import com.bienCriollas.stock.enums.TipoEgreso;
import com.bienCriollas.stock.model.Egreso;

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