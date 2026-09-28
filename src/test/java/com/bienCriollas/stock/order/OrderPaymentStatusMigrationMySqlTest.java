package com.bienCriollas.stock.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "orderPayment.mysqlTestUrl", matches = "jdbc:mysql:.*")
class OrderPaymentStatusMigrationMySqlTest {

    @Test
    void addsNonNullFalseDefaultWithoutInferringHistoricalPayments() throws Exception {
        String url = System.getProperty("orderPayment.mysqlTestUrl");
        String user = System.getProperty("orderPayment.mysqlTestUser", "root");
        String password = System.getProperty("orderPayment.mysqlTestPassword", "");

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE pedido (
                        id_pedido BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        nombre_cliente VARCHAR(255) NOT NULL,
                        estado VARCHAR(30) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    INSERT INTO pedido (nombre_cliente, estado)
                    VALUES ('Histórico entregado', 'ENTREGADO'),
                           ('Histórico pendiente', 'PENDIENTE')
                    """);

            Flyway baseline = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").baselineVersion("18").load();
            baseline.baseline();
            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("19").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);

            try (var rows = statement.executeQuery(
                    "SELECT estado, pagado FROM pedido ORDER BY id_pedido")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("estado")).isEqualTo("ENTREGADO");
                assertThat(rows.getBoolean("pagado")).isFalse();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBoolean("pagado")).isFalse();
            }

            statement.executeUpdate("""
                    INSERT INTO pedido (nombre_cliente, estado)
                    VALUES ('Nuevo sin valor explícito', 'PENDIENTE')
                    """);
            try (var row = statement.executeQuery(
                    "SELECT pagado FROM pedido WHERE id_pedido = 3")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getBoolean("pagado")).isFalse();
            }
        }
    }
}
