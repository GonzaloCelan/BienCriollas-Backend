package com.bienCriollas.stock.statistics.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bienCriollas.stock.statistics.dto.StatisticsSummaryDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StatisticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<OrderCustomerSale> getDeliveredParticularCustomerSales(LocalDate start, LocalDate end) {
        // One row per order prevents its amount/count from being multiplied by its details.
        String sql = """
                SELECT p.id_pedido, p.nombre_cliente,
                       COALESCE(p.fecha_entrega, p.fecha_pedido) AS fecha_comercial,
                       p.total_pedido,
                       COALESCE(SUM(dp.cantidad), 0) AS total_unidades
                FROM pedido p
                LEFT JOIN pedido_detalle dp ON dp.id_pedido = p.id_pedido
                WHERE p.tipo_venta = 'PARTICULAR'
                  AND p.estado = 'ENTREGADO'
                  AND COALESCE(p.fecha_entrega, p.fecha_pedido) >= ?
                  AND COALESCE(p.fecha_entrega, p.fecha_pedido) < ?
                GROUP BY p.id_pedido, p.nombre_cliente,
                         COALESCE(p.fecha_entrega, p.fecha_pedido), p.total_pedido
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrderCustomerSale(
                rs.getLong("id_pedido"), rs.getString("nombre_cliente"),
                rs.getObject("fecha_comercial", LocalDate.class), rs.getBigDecimal("total_pedido"),
                rs.getLong("total_unidades")), start, end);
    }

    public record OrderCustomerSale(long orderId, String customer, LocalDate commercialDate,
                                    BigDecimal totalSales, long totalUnits) {}

    public List<OrderTimeSale> getDeliveredOrderTimes(LocalDateTime start, LocalDateTime end) {
        String sql = """
                SELECT created_at, total_pedido
                FROM pedido
                WHERE estado = 'ENTREGADO'
                  AND created_at IS NOT NULL
                  AND created_at >= ?
                  AND created_at < ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrderTimeSale(
                rs.getObject("created_at", LocalDateTime.class),
                rs.getBigDecimal("total_pedido")), start, end);
    }

    public record OrderTimeSale(LocalDateTime createdAt, BigDecimal totalSales) {}

    public Integer countDeliveredOrders(LocalDate start, LocalDate end) {
        String sql = """
            SELECT COALESCE(COUNT(*), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND COALESCE(fecha_entrega, fecha_pedido) >= ?
              AND COALESCE(fecha_entrega, fecha_pedido) < ?
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, start, end);
    }

    public Integer countSoldEmpanadas(LocalDate start, LocalDate end) {
        String sql = """
            SELECT COALESCE(SUM(dp.cantidad), 0)
            FROM pedido_detalle dp
            INNER JOIN pedido p ON p.id_pedido = dp.id_pedido
            WHERE p.estado = 'ENTREGADO'
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) >= ?
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) < ?
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, start, end);
    }

    public BigDecimal sumTotalSales(LocalDate start, LocalDate end) {
        String sql = """
            SELECT COALESCE(SUM(total_pedido), 0)
            FROM pedido
            WHERE estado = 'ENTREGADO'
              AND COALESCE(fecha_entrega, fecha_pedido) >= ?
              AND COALESCE(fecha_entrega, fecha_pedido) < ?
        """;

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, start, end);
    }

    public List<StatisticsSummaryDTO.VarietyRankingDTO> getVarietyRanking(
            LocalDate start,
            LocalDate end
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
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) >= ?
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) < ?
            GROUP BY v.id_variedad, v.nombre
            ORDER BY unidades_vendidas DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new StatisticsSummaryDTO.VarietyRankingDTO(
                        rs.getLong("id_variedad"),
                        rs.getString("nombre"),
                        rs.getInt("unidades_vendidas")
                ),
                start,
                end
        );
    }

   public List<StatisticsSummaryDTO.SalesByWeekdayDTO> getSalesByWeekday(
        LocalDate start,
        LocalDate end
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
                DAYOFWEEK(COALESCE(p.fecha_entrega, p.fecha_pedido)) AS dia_semana,
                CASE DAYOFWEEK(COALESCE(p.fecha_entrega, p.fecha_pedido))
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
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) >= ?
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) < ?
            GROUP BY
                p.id_pedido,
                COALESCE(p.fecha_entrega, p.fecha_pedido),
                p.total_pedido
        ) t
        GROUP BY
            t.dia_semana,
            t.nombre_dia
        ORDER BY t.dia_semana
    """;

    return jdbcTemplate.query(sql, (rs, rowNum) ->
            new StatisticsSummaryDTO.SalesByWeekdayDTO(
                    rs.getInt("dia_semana"),
                    rs.getString("nombre_dia"),
                    rs.getInt("cantidad_pedidos"),
                    rs.getInt("unidades_vendidas"),
                    rs.getBigDecimal("total_vendido")
            ),
            start,
            end
    );
}
    public List<StatisticsSummaryDTO.SaleTypeSummaryDTO> getSaleTypes(
            LocalDate start,
            LocalDate end
    ) {
        String sql = """
            SELECT
                p.tipo_venta,
                COUNT(*) AS cantidad_pedidos,
                ROUND((COUNT(*) * 100.0) / NULLIF((
                    SELECT COUNT(*)
                    FROM pedido p2
                    WHERE p2.estado = 'ENTREGADO'
                      AND COALESCE(p2.fecha_entrega, p2.fecha_pedido) >= ?
                      AND COALESCE(p2.fecha_entrega, p2.fecha_pedido) < ?
                ), 0), 2) AS porcentaje
            FROM pedido p
            WHERE p.estado = 'ENTREGADO'
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) >= ?
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) < ?
            GROUP BY p.tipo_venta
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new StatisticsSummaryDTO.SaleTypeSummaryDTO(
                        rs.getString("tipo_venta"),
                        rs.getInt("cantidad_pedidos"),
                        rs.getBigDecimal("porcentaje")
                ),
                start,
                end,
                start,
                end
        );
    }

    public List<StatisticsSummaryDTO.PaymentMethodSummaryDTO> getPaymentMethods(
            LocalDate start,
            LocalDate end
    ) {
        String sql = """
            SELECT
                p.tipo_pago,
                COUNT(*) AS cantidad_pedidos,
                ROUND((COUNT(*) * 100.0) / NULLIF((
                    SELECT COUNT(*)
                    FROM pedido p2
                    WHERE p2.estado = 'ENTREGADO'
                      AND COALESCE(p2.fecha_entrega, p2.fecha_pedido) >= ?
                      AND COALESCE(p2.fecha_entrega, p2.fecha_pedido) < ?
                ), 0), 2) AS porcentaje
            FROM pedido p
            WHERE p.estado = 'ENTREGADO'
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) >= ?
              AND COALESCE(p.fecha_entrega, p.fecha_pedido) < ?
            GROUP BY p.tipo_pago
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new StatisticsSummaryDTO.PaymentMethodSummaryDTO(
                        rs.getString("tipo_pago"),
                        rs.getInt("cantidad_pedidos"),
                        rs.getBigDecimal("porcentaje")
                ),
                start,
                end,
                start,
                end
        );
    }

    public List<StatisticsSummaryDTO.WasteByVarietyDTO> getWasteByVariety(
            LocalDate start,
            LocalDate end
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
                new StatisticsSummaryDTO.WasteByVarietyDTO(
                        rs.getLong("id_variedad"),
                        rs.getString("nombre"),
                        rs.getInt("unidades_perdidas")
                ),
                start,
                end
        );
    }
}
