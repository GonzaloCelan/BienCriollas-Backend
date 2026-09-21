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
@EnabledIfSystemProperty(named = "measurement.mysqlTestUrl", matches = "jdbc:mysql:.*")
class MeasurementUnitMigrationMySqlTest {

    @Test
    void migratesLegacyQuantitiesAndPreservesHistoricalCostSnapshots() throws Exception {
        String url = System.getProperty("measurement.mysqlTestUrl");
        String user = System.getProperty("measurement.mysqlTestUser", "root");
        String password = System.getProperty("measurement.mysqlTestPassword", "");
        Flyway baseline = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("13").target("13").load();
        baseline.baseline();

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE ingredients (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        current_stock_grams DECIMAL(14,2) NOT NULL,
                        minimum_stock_grams DECIMAL(14,2) NOT NULL,
                        cost_per_kilogram DECIMAL(14,3) NOT NULL,
                        CONSTRAINT ck_ingredients_current_stock CHECK (current_stock_grams >= 0),
                        CONSTRAINT ck_ingredients_minimum_stock CHECK (minimum_stock_grams >= 0),
                        CONSTRAINT ck_ingredients_cost_per_kilogram CHECK (cost_per_kilogram > 0)
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    CREATE TABLE recipe_ingredients (
                        id BIGINT PRIMARY KEY,
                        ingredient_id BIGINT NOT NULL,
                        quantity_grams DECIMAL(14,2) NOT NULL,
                        CONSTRAINT ck_recipe_ingredient_quantity CHECK (quantity_grams > 0)
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    CREATE TABLE production_ingredients (
                        id BIGINT PRIMARY KEY,
                        ingredient_id BIGINT NOT NULL,
                        expected_quantity_grams DECIMAL(14,2) NOT NULL,
                        actual_quantity_grams DECIMAL(14,2) NULL,
                        cost_per_gram_snapshot DECIMAL(14,6) NOT NULL,
                        CONSTRAINT ck_production_ingredient_expected
                            CHECK (expected_quantity_grams >= 0),
                        CONSTRAINT ck_production_ingredient_actual
                            CHECK (actual_quantity_grams IS NULL OR actual_quantity_grams >= 0),
                        CONSTRAINT ck_production_ingredient_cost
                            CHECK (cost_per_gram_snapshot > 0)
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate(
                    "INSERT INTO ingredients VALUES (1, 'Carne', 5000, 500, 9999)");
            statement.executeUpdate(
                    "INSERT INTO recipe_ingredients VALUES (1, 1, 1000)");
            statement.executeUpdate(
                    "INSERT INTO production_ingredients VALUES (1, 1, 1000, 1100, 9.999000)");

            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("14").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);

            try (var result = statement.executeQuery("SELECT * FROM ingredients")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("measurement_unit")).isEqualTo("GRAM");
                assertThat(result.getBigDecimal("current_stock")).isEqualByComparingTo("5000");
                assertThat(result.getBigDecimal("minimum_stock")).isEqualByComparingTo("500");
                assertThat(result.getBigDecimal("cost_per_base_unit"))
                        .isEqualByComparingTo("9.999");
            }
            try (var result = statement.executeQuery("SELECT * FROM recipe_ingredients")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBigDecimal("quantity")).isEqualByComparingTo("1000");
            }
            try (var result = statement.executeQuery("SELECT * FROM production_ingredients")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("measurement_unit_snapshot")).isEqualTo("GRAM");
                assertThat(result.getBigDecimal("expected_quantity"))
                        .isEqualByComparingTo("1000");
                assertThat(result.getBigDecimal("actual_quantity"))
                        .isEqualByComparingTo("1100");
                assertThat(result.getBigDecimal("cost_per_base_unit_snapshot"))
                        .isEqualByComparingTo("9.999");
            }
            assertColumnMissing(connection, "ingredients", "cost_per_kilogram");
            assertColumnMissing(connection, "recipe_ingredients", "quantity_grams");
            assertColumnMissing(connection, "production_ingredients", "cost_per_gram_snapshot");
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE ingredients SET measurement_unit = 'LITER' WHERE id = 1"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_ingredients_measurement_unit");
        }
    }

    private void assertColumnMissing(Connection connection, String table, String column)
            throws SQLException {
        try (var columns = connection.getMetaData().getColumns(
                connection.getCatalog(), null, table, column)) {
            assertThat(columns.next()).isFalse();
        }
    }
}
