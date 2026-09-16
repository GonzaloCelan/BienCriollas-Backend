package com.bienCriollas.stock.production.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Requiere un esquema MySQL vacío dedicado a esta prueba, nunca la base de la aplicación.
 * La URL se suministra explícitamente con -Dingredient.mysqlTestUrl.
 */
@EnabledIfSystemProperty(named = "ingredient.mysqlTestUrl", matches = "jdbc:mysql:.*")
class IngredientMigrationMySqlTest {

    @Test
    void flywayConvertsExistingPricesExactlyAndKeepsConstraintsAndInventory() throws Exception {
        String url = System.getProperty("ingredient.mysqlTestUrl");
        String user = System.getProperty("ingredient.mysqlTestUser", "root");
        String password = System.getProperty("ingredient.mysqlTestPassword", "");
        Flyway previous = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("4").target("5").load();
        previous.baseline();
        previous.migrate();

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO ingredients
                        (name, current_stock_grams, minimum_stock_grams, cost_per_gram, active, created_at, updated_at)
                    VALUES
                        ('Carne', 15000, 5000, 12.500000, true, '2026-09-07 16:30:00.123456', '2026-09-07 16:30:00.123456'),
                        ('Preciso', 250.25, 0, 12.500125, false, '2026-09-07 16:30:00.123456', '2026-09-07 16:30:00.123456'),
                        ('Minimo', 1, 0, 0.000001, true, '2026-09-07 16:30:00.123456', '2026-09-07 16:30:00.123456'),
                        ('Maximo', 1, 0, 99999999.999999, true, '2026-09-07 16:30:00.123456', '2026-09-07 16:30:00.123456')
                    """);
            BigDecimal originalValue;
            try (var result = statement.executeQuery(
                    "SELECT SUM(current_stock_grams * cost_per_gram) FROM ingredients")) {
                result.next();
                originalValue = result.getBigDecimal(1);
            }

            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("6").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(current.migrate().migrationsExecuted).isZero();

            try (var result = statement.executeQuery("SELECT * FROM ingredients ORDER BY id")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBigDecimal("cost_per_kilogram")).isEqualByComparingTo("12500");
                assertThat(result.getBigDecimal("current_stock_grams")).isEqualByComparingTo("15000");
                assertThat(result.getBigDecimal("minimum_stock_grams")).isEqualByComparingTo("5000");
                assertThat(result.getTimestamp("created_at").toLocalDateTime())
                        .isEqualTo("2026-09-07T16:30:00.123456");
                assertThat(result.getTimestamp("updated_at")).isEqualTo(result.getTimestamp("created_at"));
                assertThat(result.next()).isTrue();
                assertThat(result.getBigDecimal("cost_per_kilogram")).isEqualByComparingTo("12500.125");
                assertThat(result.getBoolean("active")).isFalse();
                assertThat(result.next()).isTrue();
                assertThat(result.getBigDecimal("cost_per_kilogram")).isEqualByComparingTo("0.001");
                assertThat(result.next()).isTrue();
                assertThat(result.getBigDecimal("cost_per_kilogram")).isEqualByComparingTo("99999999999.999");
                assertThat(result.next()).isFalse();
            }
            try (var result = statement.executeQuery("""
                    SELECT SUM(current_stock_grams * cost_per_kilogram * 0.001) FROM ingredients
                    """)) {
                result.next();
                assertThat(result.getBigDecimal(1)).isEqualByComparingTo(originalValue);
            }
            try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null,
                    "ingredients", "cost_per_gram")) {
                assertThat(columns.next()).isFalse();
            }
            try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), null,
                    "ingredients", "cost_per_kilogram")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt("COLUMN_SIZE")).isEqualTo(14);
                assertThat(columns.getInt("DECIMAL_DIGITS")).isEqualTo(3);
                assertThat(columns.getInt("NULLABLE")).isZero();
            }
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE ingredients SET cost_per_kilogram = 0 WHERE name = 'Carne'"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("ck_ingredients_cost_per_kilogram");
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE ingredients SET current_stock_grams = -1 WHERE name = 'Carne'"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("ck_ingredients_current_stock");
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE ingredients SET name = 'CARNE' WHERE name = 'Preciso'"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("uk_ingredients_name");
        }
    }
}
