package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.EgresoResponseDTO;
import com.bienCriollas.stock.Dto.EgresoTipoDTO;
import com.bienCriollas.stock.Dto.EgresoTotalPorTipoDTO;
import com.bienCriollas.stock.Dto.EgresosDiariosDTO;
import com.bienCriollas.stock.Dto.EgresosPorcentajeDTO;
import com.bienCriollas.stock.Interface.EgresoMesTotalesProjection;
import com.bienCriollas.stock.Interface.EgresoTotalPorTipoProjection;
import com.bienCriollas.stock.Interface.IEgresoService;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Repository.EgresoRepository;
import com.bienCriollas.stock.enums.TipoEgreso;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EgresoService implements IEgresoService {

    private final EgresoRepository egresoRepository;

    private static final ZoneId ZONA_AR =
            ZoneId.of("America/Argentina/Buenos_Aires");

    /*
     * ==========================
     * CREACIÓN
     * ==========================
     */

    @Override
    @Transactional
    public Egreso registrarEgreso(EgresoTipoDTO request) {

        if (request == null) {
            throw new IllegalArgumentException("El egreso no puede ser nulo.");
        }

        if (request.tipoEgreso() == null) {
            throw new IllegalArgumentException("El tipo de egreso es obligatorio.");
        }

        if (request.descripcion() == null || request.descripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }

        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_AR);

        Egreso egreso = new Egreso();
        egreso.setTipoEgreso(request.tipoEgreso());
        egreso.setDescripcion(request.descripcion().trim());
        egreso.setMonto(request.monto());
        egreso.setHora(ahora.toLocalTime());
        egreso.setCreadoEn(ahora);

        return egresoRepository.save(egreso);
    }

    /*
     * ==========================
     * RESÚMENES / KPIS
     * ==========================
     */

    @Override
    @Transactional(readOnly = true)
    public EgresoResponseDTO calcularEgresoAcumulado() {

        RangoFechas rangoMesActual = obtenerRangoMesActual();

        BigDecimal totalPersonal = egresoRepository.sumarTotalPorTipoEntreFechas(
                TipoEgreso.PERSONAL,
                rangoMesActual.desde(),
                rangoMesActual.hasta()
        );

        BigDecimal totalProduccion = egresoRepository.sumarTotalPorTipoEntreFechas(
                TipoEgreso.PRODUCCION,
                rangoMesActual.desde(),
                rangoMesActual.hasta()
        );

        BigDecimal totalOtros = egresoRepository.sumarTotalPorTipoEntreFechas(
                TipoEgreso.OTROS,
                rangoMesActual.desde(),
                rangoMesActual.hasta()
        );

        return new EgresoResponseDTO(
                normalizar(totalPersonal),
                normalizar(totalProduccion),
                normalizar(totalOtros)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EgresosPorcentajeDTO> obtenerKpisMesActualVsAnterior() {

        RangoFechas mesActual = obtenerRangoMesActual();
        RangoFechas mesAnterior = obtenerRangoMesAnterior();

        List<EgresosPorcentajeDTO> resultado = new ArrayList<>();

        for (TipoEgreso tipo : TipoEgreso.values()) {

            BigDecimal totalActual = egresoRepository.sumarTotalPorTipoEntreFechas(
                    tipo,
                    mesActual.desde(),
                    mesActual.hasta()
            );

            BigDecimal totalAnterior = egresoRepository.sumarTotalPorTipoEntreFechas(
                    tipo,
                    mesAnterior.desde(),
                    mesAnterior.hasta()
            );

            BigDecimal porcentaje = calcularVariacionPorcentual(
                    normalizar(totalActual),
                    normalizar(totalAnterior)
            );

            resultado.add(
                    new EgresosPorcentajeDTO(
                            tipo,
                            normalizar(totalActual),
                            porcentaje
                    )
            );
        }

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EgresoTotalPorTipoDTO> obtenerTotalesPorTipo(int anio, int mes) {

        RangoFechas rango = obtenerRangoMes(anio, mes);

        List<EgresoTotalPorTipoProjection> rows =
                egresoRepository.obtenerTotalesPorTipoEntreFechas(
                        rango.desde(),
                        rango.hasta()
                );

        return rows.stream()
                .map(row -> new EgresoTotalPorTipoDTO(
                        row.getTipoEgreso(),
                        normalizar(row.getTotal())
                ))
                .toList();
    }

    /*
     * ==========================
     * LISTADOS
     * ==========================
     */

    @Override
    @Transactional(readOnly = true)
    public List<Egreso> obtenerEgresosDeHoy() {

        LocalDate hoy = LocalDate.now(ZONA_AR);
        RangoFechas rango = obtenerRangoDia(hoy);

        return egresoRepository
                .findByCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDesc(
                        rango.desde(),
                        rango.hasta()
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Egreso> listarHistorial(
            int anio,
            int mes,
            TipoEgreso tipoEgreso,
            Pageable pageable
    ) {
        RangoFechas rango = obtenerRangoMes(anio, mes);

        if (tipoEgreso == null) {
            return listarHistorial(anio, mes, pageable);
        }

        return egresoRepository
                .findByTipoEgresoAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDesc(
                        tipoEgreso,
                        rango.desde(),
                        rango.hasta(),
                        pageable
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Egreso> listarHistorial(
            int anio,
            int mes,
            Pageable pageable
    ) {
        RangoFechas rango = obtenerRangoMes(anio, mes);

        return egresoRepository
                .findByCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnDesc(
                        rango.desde(),
                        rango.hasta(),
                        pageable
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Egreso> listarPorTipoEgreso(
            TipoEgreso tipo,
            Pageable pageable
    ) {
        return egresoRepository.findByTipoEgresoOrderByCreadoEnDesc(
                tipo,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Egreso> obtenerUltimosMovimientos() {
        return egresoRepository.findTop5ByOrderByCreadoEnDesc();
    }

    /*
     * ==========================
     * HELPERS
     * ==========================
     */

    private RangoFechas obtenerRangoDia(LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = desde.plusDays(1);

        return new RangoFechas(desde, hasta);
    }

    private RangoFechas obtenerRangoMesActual() {
        YearMonth mesActual = YearMonth.now(ZONA_AR);
        return obtenerRangoMes(mesActual.getYear(), mesActual.getMonthValue());
    }

    private RangoFechas obtenerRangoMesAnterior() {
        YearMonth mesAnterior = YearMonth.now(ZONA_AR).minusMonths(1);
        return obtenerRangoMes(mesAnterior.getYear(), mesAnterior.getMonthValue());
    }

    private RangoFechas obtenerRangoMes(int anio, int mes) {
        YearMonth yearMonth = YearMonth.of(anio, mes);

        LocalDateTime desde = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime hasta = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        return new RangoFechas(desde, hasta);
    }

    private BigDecimal normalizar(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calcularVariacionPorcentual(
            BigDecimal actual,
            BigDecimal anterior
    ) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private record RangoFechas(
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
    }
}