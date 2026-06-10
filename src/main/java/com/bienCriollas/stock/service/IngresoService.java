package com.bienCriollas.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.ingreso.IngresoResumenDTO;
import com.bienCriollas.stock.Dto.ingreso.LiquidacionPedidosYaRequestDTO;
import com.bienCriollas.stock.repository.IngresoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IngresoService {

    private final IngresoRepository ingresoRepository;

    @Transactional(readOnly = true)
    public IngresoResumenDTO obtenerResumen(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        BigDecimal efectivo = normalizar(
                ingresoRepository.sumarEfectivoParticular(desde, hasta)
        );

        BigDecimal transferencia = normalizar(
                ingresoRepository.sumarTransferenciaParticular(desde, hasta)
        );

        BigDecimal pedidosYaEstimado = normalizar(
                ingresoRepository.sumarPedidosYaEstimado(desde, hasta)
        );

        BigDecimal liquidacionesPedidosYa = normalizar(
                ingresoRepository.sumarLiquidacionesPedidosYa(desde, hasta)
        );

        /*
         * Plata realmente recibida:
         * - efectivo de pedidos particulares
         * - transferencia de pedidos particulares
         * - liquidaciones reales recibidas de Pedidos Ya
         */
        BigDecimal totalRealRecibido = efectivo
                .add(transferencia)
                .add(liquidacionesPedidosYa);

        /*
         * Acumulado del período:
         * representa plata real ya cobrada.
         * NO incluye pedidosYaEstimado porque todavía está pendiente.
         */
        BigDecimal acumuladoPeriodo = totalRealRecibido;

        Integer cantidadPedidosParticulares =
                ingresoRepository.contarPedidosParticulares(desde, hasta);

        Integer cantidadPedidosYa =
                ingresoRepository.contarPedidosYa(desde, hasta);

        List<IngresoResumenDTO.IngresoPorDiaDTO> ingresosPorDia =
                ingresoRepository.obtenerIngresosPorDia(desde, hasta);

        /*
         * Distribución del acumulado real:
         * efectivo + transferencia + liquidaciones Pedidos Ya.
         */
        List<IngresoResumenDTO.DistribucionIngresoDTO> distribucion =
                armarDistribucion(
                        efectivo,
                        transferencia,
                        liquidacionesPedidosYa
                );

        List<IngresoResumenDTO.MovimientoIngresoDTO> movimientos =
                ingresoRepository.obtenerMovimientos(desde, hasta);

        return new IngresoResumenDTO(
                efectivo,
                transferencia,
                pedidosYaEstimado,
                liquidacionesPedidosYa,
                totalRealRecibido,
                acumuladoPeriodo,
                cantidadPedidosParticulares,
                cantidadPedidosYa,
                ingresosPorDia,
                distribucion,
                movimientos
        );
    }

    @Transactional
    public void registrarLiquidacionPedidosYa(LiquidacionPedidosYaRequestDTO request) {
        validarLiquidacion(request);
        ingresoRepository.registrarLiquidacionPedidosYa(request);
    }

    private List<IngresoResumenDTO.DistribucionIngresoDTO> armarDistribucion(
            BigDecimal efectivo,
            BigDecimal transferencia,
            BigDecimal liquidacionesPedidosYa
    ) {
        BigDecimal total = efectivo
                .add(transferencia)
                .add(liquidacionesPedidosYa);

        return List.of(
                new IngresoResumenDTO.DistribucionIngresoDTO(
                        "EFECTIVO",
                        efectivo,
                        calcularPorcentaje(efectivo, total)
                ),
                new IngresoResumenDTO.DistribucionIngresoDTO(
                        "TRANSFERENCIA",
                        transferencia,
                        calcularPorcentaje(transferencia, total)
                ),
                new IngresoResumenDTO.DistribucionIngresoDTO(
                        "LIQUIDACION_PEDIDOS_YA",
                        liquidacionesPedidosYa,
                        calcularPorcentaje(liquidacionesPedidosYa, total)
                )
        );
    }

    private BigDecimal calcularPorcentaje(BigDecimal valor, BigDecimal total) {
        if (valor == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return valor
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas desde y hasta son obligatorias");
        }

        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException("La fecha desde debe ser menor que la fecha hasta");
        }
    }

    private void validarLiquidacion(LiquidacionPedidosYaRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La liquidación no puede ser null");
        }

        if (request.fecha() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
    }

    private BigDecimal normalizar(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}