package com.bienCriollas.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.estadistica.EstadisticaResumenDTO;
import com.bienCriollas.stock.Interface.IEstadisticaService;
import com.bienCriollas.stock.repository.EstadisticaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstadisticaService implements IEstadisticaService {

    private final EstadisticaRepository estadisticaRepository;

    @Override
    @Transactional(readOnly = true)
    public EstadisticaResumenDTO obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        Integer pedidosEntregados = estadisticaRepository.contarPedidosEntregados(desde, hasta);
        Integer empanadasVendidas = estadisticaRepository.contarEmpanadasVendidas(desde, hasta);
        BigDecimal totalVendido = estadisticaRepository.sumarTotalVendido(desde, hasta);

        BigDecimal ticketPromedio = calcularTicketPromedio(totalVendido, pedidosEntregados);

        List<EstadisticaResumenDTO.RankingVariedadDTO> rankingVariedades =
                estadisticaRepository.obtenerRankingVariedades(desde, hasta);

        EstadisticaResumenDTO.VariedadMasVendidaDTO variedadMasVendida =
                rankingVariedades.isEmpty()
                        ? null
                        : new EstadisticaResumenDTO.VariedadMasVendidaDTO(
                                rankingVariedades.get(0).idVariedad(),
                                rankingVariedades.get(0).nombre(),
                                rankingVariedades.get(0).unidadesVendidas()
                        );

        List<EstadisticaResumenDTO.VentaDiaSemanaDTO> ventasPorDiaSemana =
                estadisticaRepository.obtenerVentasPorDiaSemana(desde, hasta);

        List<EstadisticaResumenDTO.TipoVentaDTO> tiposVenta =
                estadisticaRepository.obtenerTiposVenta(desde, hasta);

        List<EstadisticaResumenDTO.MedioPagoDTO> mediosPago =
                estadisticaRepository.obtenerMediosPago(desde, hasta);

        List<EstadisticaResumenDTO.MermaVariedadDTO> mermasPorVariedad =
                estadisticaRepository.obtenerMermasPorVariedad(desde, hasta);

        return new EstadisticaResumenDTO(
                pedidosEntregados,
                empanadasVendidas,
                ticketPromedio,
                variedadMasVendida,
                rankingVariedades,
                ventasPorDiaSemana,
                tiposVenta,
                mediosPago,
                mermasPorVariedad
        );
    }

	//mertodos privados para validaciones y cálculos

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas desde y hasta son obligatorias");
        }

        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException("La fecha desde debe ser menor que la fecha hasta");
        }
    }

    private BigDecimal calcularTicketPromedio(BigDecimal totalVendido, Integer pedidosEntregados) {
        if (pedidosEntregados == null || pedidosEntregados == 0) {
            return BigDecimal.ZERO;
        }

        return normalizar(totalVendido)
                .divide(BigDecimal.valueOf(pedidosEntregados), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizar(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}