package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class OrderCreatedAtMigrationTest {

    @Test
    void addsANullableColumnAndIndexWithoutInventingHistoricalTimestamps() throws Exception {
        String url = "jdbc:h2:mem:order-created-at-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE pedido (
                        id_pedido BIGINT PRIMARY KEY, nombre_cliente VARCHAR(255),
                        total_pedido DECIMAL(12,2), tipo_venta VARCHAR(20), estado VARCHAR(20),
                        fecha_pedido DATE, fecha_entrega DATE, hora_entrega TIME,
                        stock_discounted BOOLEAN
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO pedido VALUES
                        (1, 'Julieta Vargas', 18000, 'PARTICULAR', 'ENTREGADO',
                         '2025-12-10', NULL, '21:00:00', TRUE),
                        (2, 'Jorge Colman', 9000, 'PEDIDOS_YA', 'PENDIENTE',
                         '2026-09-17', '2026-09-20', '22:00:00', FALSE)
                    """);

            Flyway flyway = Flyway.configure().dataSource(url, "sa", "")
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true).baselineVersion("12").target("13").load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(flyway.migrate().migrationsExecuted).isZero();

            try (var rows = statement.executeQuery("SELECT * FROM pedido ORDER BY id_pedido")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject("created_at")).isNull();
                assertThat(rows.getString("nombre_cliente")).isEqualTo("Julieta Vargas");
                assertThat(rows.getBigDecimal("total_pedido")).isEqualByComparingTo("18000");
                assertThat(rows.getString("tipo_venta")).isEqualTo("PARTICULAR");
                assertThat(rows.getString("estado")).isEqualTo("ENTREGADO");
                assertThat(rows.getString("fecha_pedido")).isEqualTo("2025-12-10");
                assertThat(rows.getObject("fecha_entrega")).isNull();
                assertThat(rows.getString("hora_entrega")).isEqualTo("21:00:00");
                assertThat(rows.getBoolean("stock_discounted")).isTrue();

                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject("created_at")).isNull();
                assertThat(rows.getString("nombre_cliente")).isEqualTo("Jorge Colman");
                assertThat(rows.getBigDecimal("total_pedido")).isEqualByComparingTo("9000");
                assertThat(rows.getString("tipo_venta")).isEqualTo("PEDIDOS_YA");
                assertThat(rows.getString("estado")).isEqualTo("PENDIENTE");
                assertThat(rows.getString("fecha_pedido")).isEqualTo("2026-09-17");
                assertThat(rows.getString("fecha_entrega")).isEqualTo("2026-09-20");
                assertThat(rows.getString("hora_entrega")).isEqualTo("22:00:00");
                assertThat(rows.getBoolean("stock_discounted")).isFalse();
                assertThat(rows.next()).isFalse();
            }

            try (var column = connection.getMetaData().getColumns(null, null, "PEDIDO", "CREATED_AT")) {
                assertThat(column.next()).isTrue();
                assertThat(column.getString("IS_NULLABLE")).isEqualTo("YES");
                assertThat(column.getString("COLUMN_DEF")).isNull();
            }
            boolean foundIndex = false;
            try (var indexes = connection.getMetaData().getIndexInfo(null, null, "PEDIDO", false, false)) {
                while (indexes.next()) {
                    if ("idx_pedido_created_at".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        assertThat(indexes.getString("COLUMN_NAME")).isEqualToIgnoringCase("created_at");
                        foundIndex = true;
                    }
                }
            }
            assertThat(foundIndex).isTrue();
        }
    }
}
