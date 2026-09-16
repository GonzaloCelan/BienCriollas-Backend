package com.bienCriollas.stock.production.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "recipe.mysqlTestUrl", matches = "jdbc:mysql:.*")
class RecipeMigrationMySqlTest {

    @Test
    void migrationCreatesRelationsChecksAndOneActiveRecipePerVariety() throws Exception {
        String url = System.getProperty("recipe.mysqlTestUrl");
        String user = setting("recipe.mysqlTestUser", "RECIPE_MYSQL_TEST_USER", "root");
        String password = setting("recipe.mysqlTestPassword", "RECIPE_MYSQL_TEST_PASSWORD", "");

        Flyway flyway = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("6").target("6").load();
        flyway.baseline();

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
            statement.executeUpdate("INSERT INTO variedad_empanada VALUES (1, 'Carne')");
            statement.executeUpdate("""
                    INSERT INTO ingredients VALUES
                        (1, 'Carne picada', 25000, 5000, 12500, true, NOW(6), NOW(6)),
                        (2, 'Cebolla', 6000, 1000, 1350, true, NOW(6), NOW(6))
                    """);

            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("7").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(current.migrate().migrationsExecuted).isZero();

            statement.executeUpdate("""
                    INSERT INTO recipes
                        (variety_id, version, base_yield_units, notes, active, created_at, updated_at)
                    VALUES (1, 1, 100, 'Inicial', true, NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity_grams)
                    VALUES (1, 1, 8000), (1, 2, 4000)
                    """);

            try (var result = statement.executeQuery("""
                    SELECT r.version, r.active_variety_id, COUNT(ri.id)
                    FROM recipes r
                    JOIN recipe_ingredients ri ON ri.recipe_id = r.id
                    GROUP BY r.id, r.version, r.active_variety_id
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
                assertThat(result.getLong(2)).isEqualTo(1);
                assertThat(result.getInt(3)).isEqualTo(2);
            }

            assertSqlRejected(statement, """
                    INSERT INTO recipes
                        (variety_id, version, base_yield_units, active, created_at, updated_at)
                    VALUES (1, 2, 100, true, NOW(6), NOW(6))
                    """, "uk_recipe_active_variety");
            statement.executeUpdate("UPDATE recipes SET active = false WHERE id = 1");
            statement.executeUpdate("""
                    INSERT INTO recipes
                        (variety_id, version, base_yield_units, active, created_at, updated_at)
                    VALUES (1, 2, 100, true, NOW(6), NOW(6))
                    """);
            assertSqlRejected(statement, """
                    INSERT INTO recipes
                        (variety_id, version, base_yield_units, active, created_at, updated_at)
                    VALUES (1, 2, 100, false, NOW(6), NOW(6))
                    """, "uk_recipe_variety_version");
            assertSqlRejected(statement,
                    "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity_grams) "
                            + "SELECT id, 1, 0 FROM recipes WHERE version = 2",
                    "ck_recipe_ingredient_quantity");
            assertSqlRejected(statement,
                    "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity_grams) "
                            + "SELECT id, 999, 1 FROM recipes WHERE version = 2",
                    "fk_recipe_ingredient_ingredient");
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
