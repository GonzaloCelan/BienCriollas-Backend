package com.bienCriollas.stock.Dto.estadistica;

import java.math.BigDecimal;
import java.util.List;

public record EstadisticaResumenDTO(
        Integer pedidosEntregados,
        Integer empanadasVendidas,
        BigDecimal ticketPromedio,
        VariedadMasVendidaDTO variedadMasVendida,

        List<RankingVariedadDTO> rankingVariedades,
        List<VentaDiaSemanaDTO> ventasPorDiaSemana,
        List<TipoVentaDTO> tiposVenta,
        List<MedioPagoDTO> mediosPago,
        List<MermaVariedadDTO> mermasPorVariedad
) {

    public record VariedadMasVendidaDTO(
            Long idVariedad,
            String nombre,
            Integer unidadesVendidas
    ) {
    }

    public record RankingVariedadDTO(
            Long idVariedad,
            String nombre,
            Integer unidadesVendidas
    ) {
    }

    public record VentaDiaSemanaDTO(
            Integer diaSemana,
            String nombreDia,
            Integer cantidadPedidos,
            Integer unidadesVendidas,
            BigDecimal totalVendido
    ) {
    }

    public record TipoVentaDTO(
            String tipoVenta,
            Integer cantidadPedidos,
            BigDecimal porcentaje
    ) {
    }

    public record MedioPagoDTO(
            String medioPago,
            Integer cantidadPedidos,
            BigDecimal porcentaje
    ) {
    }

    public record MermaVariedadDTO(
            Long idVariedad,
            String nombre,
            Integer unidadesPerdidas
    ) {
    }
}