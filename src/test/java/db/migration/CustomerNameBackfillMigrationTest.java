package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class CustomerNameBackfillMigrationTest {

    @Test
    void flywayNormalizesOnlyTheCustomerNameOfHistoricalOrders() throws Exception {
        String url = "jdbc:h2:mem:customer-name-backfill;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE pedido (
                            id_pedido BIGINT PRIMARY KEY,
                            nombre_cliente VARCHAR(255),
                            total_pedido DECIMAL(12,2),
                            tipo_venta VARCHAR(20),
                            estado VARCHAR(20)
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO pedido
                            (id_pedido, nombre_cliente, total_pedido, tipo_venta, estado)
                        VALUES
                            (1, 'JULIETA VARGAS', 12000.50, 'PARTICULAR', 'PENDIENTE'),
                            (2, '   jorge   colman ', 18000.00, 'PEDIDOS_YA', 'ENTREGADO'),
                            (3, NULL, 9000.00, 'PARTICULAR', 'PENDIENTE')
                        """);
            }

            Flyway flyway = Flyway.configure()
                    .dataSource(url, "sa", "")
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("9")
                    .target("10")
                    .load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(flyway.migrate().migrationsExecuted).isZero();

            try (var statement = connection.createStatement();
                    var result = statement.executeQuery("""
                            SELECT id_pedido, nombre_cliente, total_pedido, tipo_venta, estado
                            FROM pedido ORDER BY id_pedido
                            """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("nombre_cliente")).isEqualTo("Julieta Vargas");
                assertThat(result.getBigDecimal("total_pedido")).isEqualByComparingTo("12000.50");
                assertThat(result.getString("tipo_venta")).isEqualTo("PARTICULAR");
                assertThat(result.getString("estado")).isEqualTo("PENDIENTE");

                assertThat(result.next()).isTrue();
                assertThat(result.getString("nombre_cliente")).isEqualTo("Jorge Colman");
                assertThat(result.getBigDecimal("total_pedido")).isEqualByComparingTo("18000.00");
                assertThat(result.getString("tipo_venta")).isEqualTo("PEDIDOS_YA");
                assertThat(result.getString("estado")).isEqualTo("ENTREGADO");

                assertThat(result.next()).isTrue();
                assertThat(result.getString("nombre_cliente")).isNull();
                assertThat(result.next()).isFalse();
            }
        }
    }
}
