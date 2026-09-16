package com.bienCriollas.stock.income.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.bienCriollas.stock.income.dto.IncomeSummaryDTO;

class IncomeRepositoryTest {

    @Test
    void attributesScheduledOrdersToTheirDeliveryDate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:income-operational-date;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE pedido (
                    id_pedido BIGINT AUTO_INCREMENT PRIMARY KEY,
                    nombre_cliente VARCHAR(100),
                    tipo_venta VARCHAR(20) NOT NULL,
                    tipo_pago VARCHAR(20),
                    monto_efectivo DECIMAL(12,2) NOT NULL,
                    monto_transferencia DECIMAL(12,2) NOT NULL,
                    total_pedido DECIMAL(12,2) NOT NULL,
                    estado VARCHAR(20) NOT NULL,
                    fecha_pedido DATE,
                    fecha_entrega DATE
                )
                """);

        LocalDate today = LocalDate.of(2026, 9, 13);
        insertDeliveredOrder(jdbcTemplate, new BigDecimal("100"), today, null);
        insertDeliveredOrder(jdbcTemplate, new BigDecimal("200"), today.minusDays(3), today);
        insertDeliveredOrder(jdbcTemplate, new BigDecimal("300"), today.minusDays(3), today.plusDays(1));
        insertDeliveredOrder(jdbcTemplate, new BigDecimal("400"), today.minusDays(1), null);

        IncomeRepository repository = new IncomeRepository(jdbcTemplate);
        BigDecimal cash = repository.sumDirectCash(today, today.plusDays(1));
        Integer orders = repository.countDirectOrders(today, today.plusDays(1));
        List<IncomeSummaryDTO.IncomeByDayDTO> incomeByDay =
                repository.getIncomeByDay(today, today.plusDays(1));

        assertEquals(0, new BigDecimal("300").compareTo(cash));
        assertEquals(2, orders);
        assertEquals(1, incomeByDay.size());
        assertEquals(today, incomeByDay.get(0).date());
        assertEquals(0, new BigDecimal("300").compareTo(incomeByDay.get(0).total()));
    }

    @Test
    void convertsUtcSettlementToArgentinaTime() {
        LocalDateTime storedUtc = LocalDateTime.of(2026, 8, 29, 2, 22);

        LocalDateTime argentinaTime = IncomeRepository.convertToArgentinaDateTime(
                "LIQUIDACION_PEDIDOS_YA",
                storedUtc);

        assertEquals(LocalDateTime.of(2026, 8, 28, 23, 22), argentinaTime);
    }

    @Test
    void doesNotConvertAnOrdersOperationalDate() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 8, 29, 0, 0);

        LocalDateTime result = IncomeRepository.convertToArgentinaDateTime(
                "PEDIDO",
                orderDate);

        assertEquals(orderDate, result);
    }

    @Test
    void preservesNullDate() {
        assertNull(IncomeRepository.convertToArgentinaDateTime(
                "LIQUIDACION_PEDIDOS_YA",
                null));
    }

    private void insertDeliveredOrder(
            JdbcTemplate jdbcTemplate,
            BigDecimal amount,
            LocalDate creationDate,
            LocalDate deliveryDate) {
        jdbcTemplate.update("""
                INSERT INTO pedido (
                    nombre_cliente,
                    tipo_venta,
                    tipo_pago,
                    monto_efectivo,
                    monto_transferencia,
                    total_pedido,
                    estado,
                    fecha_pedido,
                    fecha_entrega
                ) VALUES (?, 'PARTICULAR', 'EFECTIVO', ?, 0, ?, 'ENTREGADO', ?, ?)
                """,
                "Cliente",
                amount,
                amount,
                creationDate,
                deliveryDate);
    }
}
