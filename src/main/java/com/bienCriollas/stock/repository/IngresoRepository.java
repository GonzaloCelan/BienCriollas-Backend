package com.bienCriollas.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.Dto.ingreso.IngresoResumenDTO;
import com.bienCriollas.stock.Dto.ingreso.LiquidacionPedidosYaRequestDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IngresoRepository {

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal sumarEfectivoParticular(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(SUM(monto_efectivo), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND tipo_venta = 'PARTICULAR'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, desde, hasta);
    }

    public BigDecimal sumarTransferenciaParticular(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(SUM(monto_transferencia), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND tipo_venta = 'PARTICULAR'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, desde, hasta);
    }

    public BigDecimal sumarPedidosYaEstimado(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(SUM(total_pedido), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND tipo_venta = 'PEDIDOS_YA'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, desde, hasta);
    }

    public BigDecimal sumarLiquidacionesPedidosYa(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(SUM(monto), 0)
            FROM liquidacion_pedidos_ya
            WHERE fecha >= ?
              AND fecha < ?
        """;

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, desde, hasta);
    }

    public Integer contarPedidosParticulares(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(COUNT(*), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND tipo_venta = 'PARTICULAR'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, desde, hasta);
    }

    public Integer contarPedidosYa(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT COALESCE(COUNT(*), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND tipo_venta = 'PEDIDOS_YA'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, desde, hasta);
    }

    public List<IngresoResumenDTO.IngresoPorDiaDTO> obtenerIngresosPorDia(
            LocalDate desde,
            LocalDate hasta
    ) {
        String sql = """
            SELECT
                DATE(fecha_pedido) AS fecha,
                COALESCE(SUM(CASE
                    WHEN tipo_venta = 'PARTICULAR' THEN monto_efectivo
                    ELSE 0
                END), 0) AS efectivo,
                COALESCE(SUM(CASE
                    WHEN tipo_venta = 'PARTICULAR' THEN monto_transferencia
                    ELSE 0
                END), 0) AS transferencia,
                COALESCE(SUM(CASE
                    WHEN tipo_venta = 'PEDIDOS_YA' THEN total_pedido
                    ELSE 0
                END), 0) AS pedidos_ya_estimado,
                COALESCE(SUM(CASE
                    WHEN tipo_venta = 'PARTICULAR' THEN monto_efectivo + monto_transferencia
                    WHEN tipo_venta = 'PEDIDOS_YA' THEN total_pedido
                    ELSE 0
                END), 0) AS total
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND fecha_pedido >= ?
              AND fecha_pedido < ?
            GROUP BY DATE(fecha_pedido)
            ORDER BY DATE(fecha_pedido)
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new IngresoResumenDTO.IngresoPorDiaDTO(
                        rs.getDate("fecha").toLocalDate(),
                        rs.getBigDecimal("efectivo"),
                        rs.getBigDecimal("transferencia"),
                        rs.getBigDecimal("pedidos_ya_estimado"),
                        rs.getBigDecimal("total")
                ),
                desde,
                hasta
        );
    }
public List<IngresoResumenDTO.MovimientoIngresoDTO> obtenerMovimientos(
        LocalDate desde,
        LocalDate hasta
) {
    String sql = """
        SELECT
            p.id_pedido AS id,
            p.id_pedido AS id_pedido,
            DATE(p.fecha_pedido) AS fecha,
            CAST(p.fecha_pedido AS DATETIME) AS fecha_hora,

            CONVERT(
                CASE
                    WHEN p.tipo_venta = 'PEDIDOS_YA'
                        THEN CONCAT('Pedido Ya #', p.id_pedido)
                    ELSE CONCAT('Pedido #', p.id_pedido, ' - ', p.nombre_cliente)
                END USING utf8mb4
            ) COLLATE utf8mb4_unicode_ci AS descripcion,

            CONVERT(p.tipo_venta USING utf8mb4) COLLATE utf8mb4_unicode_ci AS tipo_venta,

            CONVERT(
                CASE
                    WHEN p.tipo_venta = 'PEDIDOS_YA' THEN 'PEDIDOS_YA'
                    ELSE p.tipo_pago
                END USING utf8mb4
            ) COLLATE utf8mb4_unicode_ci AS medio_pago,

            CASE
                WHEN p.tipo_venta = 'PEDIDOS_YA' THEN p.total_pedido
                ELSE COALESCE(p.monto_efectivo, 0) + COALESCE(p.monto_transferencia, 0)
            END AS monto,

            CONVERT(
                CASE
                    WHEN p.tipo_venta = 'PEDIDOS_YA' THEN 'PENDIENTE_LIQUIDACION'
                    ELSE 'COBRADO'
                END USING utf8mb4
            ) COLLATE utf8mb4_unicode_ci AS estado_ingreso,

            CONVERT('PEDIDO' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS origen

        FROM pedido p
        WHERE p.estado = 'ENTREGADO'
          AND p.fecha_pedido >= ?
          AND p.fecha_pedido < ?

        UNION ALL

        SELECT
            l.id_liquidacion AS id,
            NULL AS id_pedido,
            l.fecha AS fecha,
            l.creado_en AS fecha_hora,

            CONVERT(
                COALESCE(l.descripcion, 'Liquidación Pedidos Ya') USING utf8mb4
            ) COLLATE utf8mb4_unicode_ci AS descripcion,

            CONVERT('PEDIDOS_YA' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS tipo_venta,

            CONVERT('LIQUIDACION' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS medio_pago,

            l.monto AS monto,

            CONVERT('LIQUIDACION_RECIBIDA' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS estado_ingreso,

            CONVERT('LIQUIDACION_PEDIDOS_YA' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS origen

        FROM liquidacion_pedidos_ya l
        WHERE l.fecha >= ?
          AND l.fecha < ?

        ORDER BY fecha_hora DESC, id DESC
    """;

    return jdbcTemplate.query(sql, (rs, rowNum) ->
            new IngresoResumenDTO.MovimientoIngresoDTO(
                    rs.getLong("id"),
                    rs.getObject("id_pedido") != null ? rs.getLong("id_pedido") : null,
                    rs.getDate("fecha").toLocalDate(),
                    rs.getTimestamp("fecha_hora") != null
                            ? rs.getTimestamp("fecha_hora").toLocalDateTime()
                            : null,
                    rs.getString("descripcion"),
                    rs.getString("tipo_venta"),
                    rs.getString("medio_pago"),
                    rs.getBigDecimal("monto"),
                    rs.getString("estado_ingreso"),
                    rs.getString("origen")
            ),
            desde,
            hasta,
            desde,
            hasta
    );
}

    public void registrarLiquidacionPedidosYa(LiquidacionPedidosYaRequestDTO request) {
        String sql = """
            INSERT INTO liquidacion_pedidos_ya (fecha, monto, descripcion)
            VALUES (?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                request.fecha(),
                request.monto(),
                request.descripcion()
        );
    }
}