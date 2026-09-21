package com.bienCriollas.stock.production;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "recipeCosts.mysqlTestUrl", matches = "jdbc:mysql:.*")
class RecipeAdditionalCostMigrationMySqlTest {

    @Test
    void createsVersionedAdditionalCostsAndProductionSnapshots() throws Exception {
        String url = System.getProperty("recipeCosts.mysqlTestUrl");
        String user = System.getProperty("recipeCosts.mysqlTestUser", "root");
        String password = System.getProperty("recipeCosts.mysqlTestPassword", "");
        Flyway baseline = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("14").target("14").load();
        baseline.baseline();

        try (Connection connection = DriverManager.getConnection(url, user, password);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE recipes (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
            statement.executeUpdate("CREATE TABLE productions (id BIGINT PRIMARY KEY) ENGINE=InnoDB");

            Flyway current = Flyway.configure().dataSource(url, user, password)
                    .locations("classpath:db/migration").target("16").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(2);

            statement.executeUpdate("INSERT INTO recipes VALUES (1)");
            statement.executeUpdate("""
                    INSERT INTO recipe_additional_costs (
                        recipe_id, cost_type, name, calculation_mode, cost_value,
                        sort_order, active, created_at, updated_at
                    ) VALUES
                        (1, 'LABOR', 'Mano de obra', 'FIXED_TOTAL', 16250, 1, TRUE, NOW(6), NOW(6)),
                        (1, 'OTHER', 'Limpieza', 'FIXED_TOTAL', 500, 2, TRUE, NOW(6), NOW(6)),
                        (1, 'OTHER', 'Administración', 'PERCENTAGE', 2, 3, TRUE, NOW(6), NOW(6))
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO recipe_additional_costs (
                        recipe_id, cost_type, name, calculation_mode, cost_value,
                        sort_order, active, created_at, updated_at
                    ) VALUES (1, 'LABOR', 'Duplicado', 'FIXED_TOTAL', 1, 4, TRUE, NOW(6), NOW(6))
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_recipe_nonrepeatable_additional_cost");

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO recipe_additional_costs (
                        recipe_id, cost_type, name, calculation_mode, cost_value,
                        sort_order, active, created_at, updated_at
                    ) VALUES (1, 'PACKAGING', 'Envase inválido', 'FIXED_TOTAL',
                        1, 4, TRUE, NOW(6), NOW(6))
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_recipe_additional_cost_combination");

            statement.executeUpdate("INSERT INTO productions VALUES (1, TRUE)");
            statement.executeUpdate("""
                    INSERT INTO production_additional_costs (
                        production_id, recipe_additional_cost_id, cost_type, name_snapshot,
                        calculation_mode_snapshot, value_snapshot,
                        calculated_expected_cost_snapshot, sort_order, created_at
                    ) VALUES (1, 1, 'LABOR', 'Mano de obra', 'FIXED_TOTAL',
                        16250, 32500, 1, NOW(6))
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO production_additional_costs (
                        production_id, cost_type, name_snapshot,
                        calculation_mode_snapshot, value_snapshot,
                        calculated_expected_cost_snapshot, sort_order, created_at
                    ) VALUES (1, 'ENERGY', 'Energía inválida', 'FIXED_TOTAL',
                        10, 10, 2, NOW(6))
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_production_additional_cost_combination");
            try (ResultSet result = statement.executeQuery(
                    "SELECT calculated_expected_cost_snapshot FROM production_additional_costs")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBigDecimal(1)).isEqualByComparingTo("32500");
            }
        }
    }
}
