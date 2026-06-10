package com.bienCriollas.stock.Dto.ingreso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record IngresoResumenDTO(
        BigDecimal efectivo,
        BigDecimal transferencia,
        BigDecimal pedidosYaEstimado,
        BigDecimal liquidacionesPedidosYa,

        BigDecimal totalRealRecibido,
        BigDecimal acumuladoPeriodo,

        Integer cantidadPedidosParticulares,
        Integer cantidadPedidosYa,

        List<IngresoPorDiaDTO> ingresosPorDia,
        List<DistribucionIngresoDTO> distribucion,
        List<MovimientoIngresoDTO> movimientos
) {

    public record IngresoPorDiaDTO(
            LocalDate fecha,
            BigDecimal efectivo,
            BigDecimal transferencia,
            BigDecimal pedidosYaEstimado,
            BigDecimal total
    ) {
    }

    public record DistribucionIngresoDTO(
            String tipo,
            BigDecimal monto,
            BigDecimal porcentaje
    ) {
    }

    public record MovimientoIngresoDTO(
            Long id,
            Long idPedido,
            LocalDate fecha,
            LocalDateTime fechaHora,
            String descripcion,
            String tipoVenta,
            String medioPago,
            BigDecimal monto,
            String estadoIngreso,
            String origen
    ) {
    }
}