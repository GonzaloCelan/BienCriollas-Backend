package com.bienCriollas.stock.production.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "ingredientPurchase.mysqlTestUrl", matches = "jdbc:mysql:.*")
class IngredientPurchasePresentationMigrationMySqlTest {

    @Test
    void addsNullablePurchaseDataWithoutInventingOrChangingLegacyCosts() throws Exception {
        String url = System.getProperty("ingredientPurchase.mysqlTestUrl");
        String user = System.getProperty("ingredientPurchase.mysqlTestUser", "root");
        String password = System.getProperty("ingredientPurchase.mysqlTestPassword", "");

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE ingredients (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        measurement_unit VARCHAR(30) NOT NULL,
                        current_stock DECIMAL(19,4) NOT NULL,
                        minimum_stock DECIMAL(19,4) NOT NULL,
                        cost_per_base_unit DECIMAL(19,6) NOT NULL,
                        active BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.executeUpdate("""
                    INSERT INTO ingredients (
                        name, measurement_unit, current_stock, minimum_stock,
                        cost_per_base_unit, active, created_at, updated_at
                    ) VALUES ('Aceite histórico', 'MILLILITER', 900, 300,
                        3.044444, TRUE, NOW(6), NOW(6))
                    """);

            Flyway baseline = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").baselineVersion("16").load();
            baseline.baseline();
            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("17").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);

            try (var result = statement.executeQuery("SELECT * FROM ingredients WHERE id = 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("purchase_presentation")).isNull();
                assertThat(result.getBigDecimal("purchase_quantity")).isNull();
                assertThat(result.getBigDecimal("purchase_price")).isNull();
                assertThat(result.getBigDecimal("cost_per_base_unit"))
                        .isEqualByComparingTo("3.044444");
                assertThat(result.getBigDecimal("current_stock")).isEqualByComparingTo("900");
            }

            statement.executeUpdate("""
                    UPDATE ingredients
                    SET purchase_presentation = 'Botella',
                        purchase_quantity = 900,
                        purchase_price = 2740
                    WHERE id = 1
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE ingredients
                    SET purchase_presentation = NULL
                    WHERE id = 1
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_ingredients_purchase_data_complete");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    UPDATE ingredients
                    SET purchase_quantity = 0
                    WHERE id = 1
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_ingredients_purchase_quantity");
        }
    }
}
