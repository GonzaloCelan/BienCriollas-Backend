package com.bienCriollas.stock.income.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class IncomeRepositoryTest {

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
}
