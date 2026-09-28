package com.bienCriollas.stock.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "orderEta.mysqlTestUrl", matches = "jdbc:mysql:.*")
class OrderDeliveryEtaMigrationMySqlTest {

    @Test
    void addsNullableDateTimeAndLeavesHistoricalOrdersWithoutEta() throws Exception {
        String url = System.getProperty("orderEta.mysqlTestUrl");
        String user = System.getProperty("orderEta.mysqlTestUser", "root");
        String password = System.getProperty("orderEta.mysqlTestPassword", "");

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE pedido (
                        id_pedido BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        nombre_cliente VARCHAR(255) NOT NULL,
                        estado VARCHAR(30) NOT NULL,
                        pagado BOOLEAN NOT NULL DEFAULT FALSE
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    INSERT INTO pedido (nombre_cliente, estado)
                    VALUES ('Pedido histórico', 'ENTREGADO')
                    """);

            Flyway baseline = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").baselineVersion("19").load();
            baseline.baseline();
            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("20").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);

            try (var row = statement.executeQuery("""
                    SELECT fecha_hora_estimada_delivery
                    FROM pedido
                    WHERE id_pedido = 1
                    """)) {
                assertThat(row.next()).isTrue();
                assertThat(row.getTimestamp("fecha_hora_estimada_delivery")).isNull();
            }

            try (var columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, "pedido", "fecha_hora_estimada_delivery")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("NULLABLE"))
                        .isEqualTo(java.sql.DatabaseMetaData.columnNullable);
            }
        }
    }
}
