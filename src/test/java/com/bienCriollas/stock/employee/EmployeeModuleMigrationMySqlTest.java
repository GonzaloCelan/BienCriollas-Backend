package com.bienCriollas.stock.employee;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Ejecutar únicamente sobre un esquema MySQL vacío y dedicado a pruebas. */
@EnabledIfSystemProperty(named = "employee.mysqlTestUrl", matches = "jdbc:mysql:.*")
class EmployeeModuleMigrationMySqlTest {

    @Test
    void createsTablesConstraintsUniquenessAndCascade() throws Exception {
        String url = System.getProperty("employee.mysqlTestUrl");
        String user = System.getProperty("employee.mysqlTestUser", "root");
        String password = System.getProperty("employee.mysqlTestPassword", "");

        Flyway baseline = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").baselineVersion("17").load();
        baseline.baseline();
        Flyway current = Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").target("18").load();
        assertThat(current.migrate().migrationsExecuted).isEqualTo(1);

        try (Connection connection = DriverManager.getConnection(url, user, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO employees
                        (name, hourly_rate, active, created_at, updated_at)
                    VALUES ('Ana', 3500, TRUE, NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO employee_work_days
                        (employee_id, work_date, hourly_rate_snapshot,
                         total_worked_minutes, total_amount, created_at, updated_at)
                    VALUES (1, '2026-09-21', 3500, 240, 14000, NOW(6), NOW(6))
                    """);
            statement.executeUpdate("""
                    INSERT INTO employee_work_shifts
                        (work_day_id, start_time, end_time, break_minutes,
                         worked_minutes, sort_order, created_at, updated_at)
                    VALUES (1, '08:00:00', '12:00:00', 0, 240, 1, NOW(6), NOW(6))
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO employee_work_days
                        (employee_id, work_date, hourly_rate_snapshot,
                         total_worked_minutes, total_amount, created_at, updated_at)
                    VALUES (1, '2026-09-21', 3500, 60, 3500, NOW(6), NOW(6))
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_employee_work_day_employee_date");
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO employees
                        (name, hourly_rate, active, created_at, updated_at)
                    VALUES ('Inválido', 0, TRUE, NOW(6), NOW(6))
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_employees_hourly_rate");

            statement.executeUpdate("DELETE FROM employee_work_days WHERE id = 1");
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM employee_work_shifts WHERE work_day_id = 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isZero();
            }
        }
    }
}
