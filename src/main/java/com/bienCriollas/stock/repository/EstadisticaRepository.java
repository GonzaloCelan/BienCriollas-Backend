package com.bienCriollas.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Dto.estadistica.EstadisticaResumenDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EstadisticaRepository {

    private final JdbcTemplate jdbcTemplate;

    public Integer contarPedidosEntregados(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(COUNT(*), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, desde, hasta);
    }

    public Integer contarEmpanadasVendidas(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(SUM(dp.cantidad), 0)
            FROM pedido_detalle dp
            INNER JOIN pedido p ON p.id_pedido = dp.id_pedido
            WHERE p.estado = 'ENTREGADO'
              AND p.fecha_pedido >= ?
              AND p.fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, desde, hasta);
    }

    public BigDecimal sumarTotalVendido(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(SUM(total_pedido), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, desde, hasta);
    }

    public List<EstadisticaResumenDTO.RankingVariedadDTO> obtenerRankingVariedades(
            LocalDate desde,
            LocalDate hasta
    ) {
        String sql = """
            SELECT
                v.id_variedad,
                v.nombre,
                COALESCE(SUM(dp.cantidad), 0) AS unidades_vendidas
            FROM pedido_detalle dp
            INNER JOIN pedido p ON p.id_pedido = dp.id_pedido
            INNER JOIN variedad_empanada v ON v.id_variedad = dp.id_variedad
            WHERE p.estado = 'ENTREGADO'
              AND p.fecha_pedido >= ?
              AND p.fecha_pedido < ?
            GROUP BY v.id_variedad, v.nombre
            ORDER BY unidades_vendidas DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EstadisticaResumenDTO.RankingVariedadDTO(
                        rs.getLong("id_variedad"),
                        rs.getString("nombre"),
                        rs.getInt("unidades_vendidas")
                ),
                desde,
                hasta
        );
    }

   public List<EstadisticaResumenDTO.VentaDiaSemanaDTO> obtenerVentasPorDiaSemana(
        LocalDate desde,
        LocalDate hasta
) {
    String sql = """
        SELECT
            t.dia_semana,
            t.nombre_dia,
            COUNT(*) AS cantidad_pedidos,
            COALESCE(SUM(t.unidades_vendidas), 0) AS unidades_vendidas,
            COALESCE(SUM(t.total_pedido), 0) AS total_vendido
        FROM (
            SELECT
                p.id_pedido,
                DAYOFWEEK(p.fecha_pedido) AS dia_semana,
                CASE DAYOFWEEK(p.fecha_pedido)
                    WHEN 1 THEN 'Domingo'
                    WHEN 2 THEN 'Lunes'
                    WHEN 3 THEN 'Martes'
                    WHEN 4 THEN 'Miércoles'
                    WHEN 5 THEN 'Jueves'
                    WHEN 6 THEN 'Viernes'
                    WHEN 7 THEN 'Sábado'
                END AS nombre_dia,
                p.total_pedido,
                COALESCE(SUM(dp.cantidad), 0) AS unidades_vendidas
            FROM pedido p
            LEFT JOIN pedido_detalle dp ON dp.id_pedido = p.id_pedido
            WHERE p.estado = 'ENTREGADO'
              AND p.fecha_pedido >= ?
              AND p.fecha_pedido < ?
            GROUP BY
                p.id_pedido,
                p.fecha_pedido,
                p.total_pedido
        ) t
        GROUP BY
            t.dia_semana,
            t.nombre_dia
        ORDER BY t.dia_semana
    """;

    return jdbcTemplate.query(sql, (rs, rowNum) ->
            new EstadisticaResumenDTO.VentaDiaSemanaDTO(
                    rs.getInt("dia_semana"),
                    rs.getString("nombre_dia"),
                    rs.getInt("cantidad_pedidos"),
                    rs.getInt("unidades_vendidas"),
                    rs.getBigDecimal("total_vendido")
            ),
            desde,
            hasta
    );
}
    public List<EstadisticaResumenDTO.TipoVentaDTO> obtenerTiposVenta(
            LocalDate desde,
            LocalDate hasta
    ) {
        String sql = """
            SELECT
                p.tipo_venta,
                COUNT(*) AS cantidad_pedidos,
                ROUND((COUNT(*) * 100.0) / NULLIF((
                    SELECT COUNT(*)
                    FROM pedido p2
                    WHERE p2.estado = 'ENTREGADO'
                      AND p2.fecha_pedido >= ?
                      AND p2.fecha_pedido < ?
                ), 0), 2) AS porcentaje
            FROM pedido p
            WHERE p.estado = 'ENTREGADO'
              AND p.fecha_pedido >= ?
              AND p.fecha_pedido < ?
            GROUP BY p.tipo_venta
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EstadisticaResumenDTO.TipoVentaDTO(
                        rs.getString("tipo_venta"),
                        rs.getInt("cantidad_pedidos"),
                        rs.getBigDecimal("porcentaje")
                ),
                desde,
                hasta,
                desde,
                hasta
        );
    }

    public List<EstadisticaResumenDTO.MedioPagoDTO> obtenerMediosPago(
            LocalDate desde,
            LocalDate hasta
    ) {
        String sql = """
            SELECT
                p.tipo_pago,
                COUNT(*) AS cantidad_pedidos,
                ROUND((COUNT(*) * 100.0) / NULLIF((
                    SELECT COUNT(*)
                    FROM pedido p2
                    WHERE p2.estado = 'ENTREGADO'
                      AND p2.fecha_pedido >= ?
                      AND p2.fecha_pedido < ?
                ), 0), 2) AS porcentaje
            FROM pedido p
            WHERE p.estado = 'ENTREGADO'
              AND p.fecha_pedido >= ?
              AND p.fecha_pedido < ?
            GROUP BY p.tipo_pago
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EstadisticaResumenDTO.MedioPagoDTO(
                        rs.getString("tipo_pago"),
                        rs.getInt("cantidad_pedidos"),
                        rs.getBigDecimal("porcentaje")
                ),
                desde,
                hasta,
                desde,
                hasta
        );
    }

    public List<EstadisticaResumenDTO.MermaVariedadDTO> obtenerMermasPorVariedad(
            LocalDate desde,
            LocalDate hasta
    ) {
        String sql = """
            SELECT
                v.id_variedad,
                v.nombre,
                COALESCE(SUM(pe.cantidad), 0) AS unidades_perdidas
            FROM merma_empanada pe
            INNER JOIN variedad_empanada v ON v.id_variedad = pe.id_variedad
            WHERE pe.fecha_registro >= ?
              AND pe.fecha_registro < ?
            GROUP BY v.id_variedad, v.nombre
            ORDER BY unidades_perdidas DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new EstadisticaResumenDTO.MermaVariedadDTO(
                        rs.getLong("id_variedad"),
                        rs.getString("nombre"),
                        rs.getInt("unidades_perdidas")
                ),
                desde,
                hasta
        );
    }
}