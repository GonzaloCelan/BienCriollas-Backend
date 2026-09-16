package com.bienCriollas.stock.production.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "process.mysqlTestUrl", matches = "jdbc:mysql:.*")
class ProductionProcessMigrationMySqlTest {

    @Test
    void migrationCreatesRelationsChecksAndOneCurrentProcessPerVariety() throws Exception {
        String url = System.getProperty("process.mysqlTestUrl");
        String user = setting("process.mysqlTestUser", "PROCESS_MYSQL_TEST_USER", "root");
        String password = setting(
                "process.mysqlTestPassword", "PROCESS_MYSQL_TEST_PASSWORD", "");

        Flyway flyway = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("7").target("7").load();
        flyway.baseline();

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE variedad_empanada (
                        id_variedad INT UNSIGNED PRIMARY KEY,
                        nombre VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("INSERT INTO variedad_empanada VALUES (1, 'Carne')");

            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("8").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(current.migrate().migrationsExecuted).isZero();

            statement.executeUpdate("""
                    INSERT INTO production_processes
                        (variety_id, version, reference_yield_units, notes,
                         active, created_at, updated_at)
                    VALUES (1, 1, 100, 'Inicial', true, NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO production_process_steps
                        (process_id, step_order, name, estimated_minutes,
                         required_people, time_type)
                    VALUES
                        (1, 1, 'Preparar', 20, 1, 'ACTIVE'),
                        (1, 2, 'Enfriar', 60, 0, 'WAITING')
                    """);

            try (var result = statement.executeQuery("""
                    SELECT p.version, p.active_variety_id, COUNT(s.id)
                    FROM production_processes p
                    JOIN production_process_steps s ON s.process_id = p.id
                    GROUP BY p.id, p.version, p.active_variety_id
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
                assertThat(result.getLong(2)).isEqualTo(1);
                assertThat(result.getInt(3)).isEqualTo(2);
            }

            assertSqlRejected(statement, """
                    INSERT INTO production_processes
                        (variety_id, version, reference_yield_units,
                         active, created_at, updated_at)
                    VALUES (1, 2, 100, true, NOW(6), NOW(6))
                    """, "uk_production_process_active_variety");
            statement.executeUpdate(
                    "UPDATE production_processes SET active = false WHERE id = 1");
            statement.executeUpdate("""
                    INSERT INTO production_processes
                        (variety_id, version, reference_yield_units,
                         active, created_at, updated_at)
                    VALUES (1, 2, 100, true, NOW(6), NOW(6))
                    """);
            assertSqlRejected(statement, """
                    INSERT INTO production_processes
                        (variety_id, version, reference_yield_units,
                         active, created_at, updated_at)
                    VALUES (1, 2, 100, false, NOW(6), NOW(6))
                    """, "uk_production_process_variety_version");
            assertSqlRejected(statement, """
                    INSERT INTO production_process_steps
                        (process_id, step_order, name, estimated_minutes,
                         required_people, time_type)
                    VALUES (2, 1, 'Armar', 50, 0, 'ACTIVE')
                    """, "ck_process_step_active_people");
            assertSqlRejected(statement, """
                    INSERT INTO production_process_steps
                        (process_id, step_order, name, estimated_minutes,
                         required_people, time_type)
                    VALUES (2, 1, 'Otro', 10, 1, 'INVALID')
                    """, "ck_process_step_time_type");
            assertSqlRejected(statement, """
                    INSERT INTO production_process_steps
                        (process_id, step_order, name, estimated_minutes,
                         required_people, time_type)
                    VALUES (999, 1, 'Otro', 10, 1, 'ACTIVE')
                    """, "fk_process_step_process");
        }
    }

    private void assertSqlRejected(java.sql.Statement statement, String sql, String constraint) {
        assertThatThrownBy(() -> statement.executeUpdate(sql))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining(constraint);
    }

    private String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null) {
            value = System.getenv(environment);
        }
        return value == null ? defaultValue : value;
    }
}
