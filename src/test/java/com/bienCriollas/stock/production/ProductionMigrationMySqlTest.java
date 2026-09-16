package com.bienCriollas.stock.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "production.mysqlTestUrl", matches = "jdbc:mysql:.*")
class ProductionMigrationMySqlTest {

    @Test
    void migrationCreatesHistoricalRelationsAndDatabaseConstraints() throws Exception {
        String url = System.getProperty("production.mysqlTestUrl");
        String user = setting("production.mysqlTestUser", "PRODUCTION_MYSQL_TEST_USER", "root");
        String password = setting(
                "production.mysqlTestPassword", "PRODUCTION_MYSQL_TEST_PASSWORD", "");

        Flyway baseline = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("8").target("8").load();
        baseline.baseline();

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE variedad_empanada (
                        id_variedad INT UNSIGNED PRIMARY KEY,
                        nombre VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    CREATE TABLE ingredients (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        current_stock_grams DECIMAL(14,2) NOT NULL,
                        minimum_stock_grams DECIMAL(14,2) NOT NULL,
                        cost_per_kilogram DECIMAL(14,3) NOT NULL,
                        active BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    CREATE TABLE recipes (
                        id BIGINT PRIMARY KEY,
                        variety_id INT UNSIGNED NOT NULL,
                        version INT NOT NULL,
                        base_yield_units INT NOT NULL,
                        active BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    CREATE TABLE production_processes (
                        id BIGINT PRIMARY KEY,
                        variety_id INT UNSIGNED NOT NULL,
                        version INT NOT NULL,
                        reference_yield_units INT NOT NULL,
                        active BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("INSERT INTO variedad_empanada VALUES (1, 'Carne')");
            statement.executeUpdate("""
                    INSERT INTO ingredients VALUES
                        (1, 'Carne picada', 25000, 5000, 12500, true, NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO recipes
                        (id, variety_id, version, base_yield_units, active, created_at, updated_at)
                    VALUES (1, 1, 3, 100, true, NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO production_processes
                        (id, variety_id, version, reference_yield_units, active, created_at, updated_at)
                    VALUES (1, 1, 2, 100, true, NOW(6), NOW(6))
                    """);

            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("9").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(current.migrate().migrationsExecuted).isZero();

            statement.executeUpdate("""
                    INSERT INTO productions
                        (variety_id, recipe_id, process_id, production_date, planned_units,
                         waste_units, status, created_at, updated_at)
                    VALUES (1, 1, 1, '2026-09-10', 100, 0, 'DRAFT', NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO production_ingredients
                        (production_id, ingredient_id, expected_quantity_grams,
                         actual_quantity_grams, cost_per_gram_snapshot)
                    VALUES (1, 1, 8000, NULL, 12.500000)
                    """);

            try (var result = statement.executeQuery("""
                    SELECT p.status, p.recipe_id, p.process_id, pi.cost_per_gram_snapshot
                    FROM productions p
                    JOIN production_ingredients pi ON pi.production_id = p.id
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("DRAFT");
                assertThat(result.getLong(2)).isEqualTo(1);
                assertThat(result.getLong(3)).isEqualTo(1);
                assertThat(result.getBigDecimal(4)).isEqualByComparingTo("12.500000");
            }

            assertSqlRejected(statement, """
                    INSERT INTO productions
                        (variety_id, recipe_id, production_date, planned_units,
                         waste_units, status, created_at, updated_at)
                    VALUES (1, 1, '2026-09-10', 0, 0, 'DRAFT', NOW(6), NOW(6))
                    """, "ck_productions_planned_units");
            assertSqlRejected(statement, """
                    INSERT INTO productions
                        (variety_id, recipe_id, production_date, planned_units,
                         waste_units, status, created_at, updated_at)
                    VALUES (1, 1, '2026-09-10', 10, 0, 'INVALID', NOW(6), NOW(6))
                    """, "ck_productions_status");
            assertSqlRejected(statement, """
                    INSERT INTO production_ingredients
                        (production_id, ingredient_id, expected_quantity_grams,
                         actual_quantity_grams, cost_per_gram_snapshot)
                    VALUES (1, 1, 1, 1, 12.5)
                    """, "uk_production_ingredient");
            assertSqlRejected(statement, """
                    INSERT INTO production_ingredients
                        (production_id, ingredient_id, expected_quantity_grams,
                         actual_quantity_grams, cost_per_gram_snapshot)
                    VALUES (999, 1, 1, 1, 12.5)
                    """, "fk_production_ingredient_production");
        }
    }

    private void assertSqlRejected(java.sql.Statement statement, String sql, String constraint) {
        assertThatThrownBy(() -> statement.executeUpdate(sql))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining(constraint);
    }

    private String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null) value = System.getenv(environment);
        return value == null ? defaultValue : value;
    }
}
