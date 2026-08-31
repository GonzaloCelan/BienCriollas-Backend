package com.bienCriollas.stock.ingreso.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class IngresoRepositoryTest {

    @Test
    void convierteLiquidacionUtcAHoraArgentina() {
        LocalDateTime almacenadaUtc = LocalDateTime.of(2026, 8, 29, 2, 22);

        LocalDateTime argentina = IngresoRepository.convertirFechaHoraArgentina(
                "LIQUIDACION_PEDIDOS_YA",
                almacenadaUtc);

        assertEquals(LocalDateTime.of(2026, 8, 28, 23, 22), argentina);
    }

    @Test
    void noConvierteLaFechaOperativaDeUnPedido() {
        LocalDateTime fechaPedido = LocalDateTime.of(2026, 8, 29, 0, 0);

        LocalDateTime resultado = IngresoRepository.convertirFechaHoraArgentina(
                "PEDIDO",
                fechaPedido);

        assertEquals(fechaPedido, resultado);
    }

    @Test
    void conservaFechaNula() {
        assertNull(IngresoRepository.convertirFechaHoraArgentina(
                "LIQUIDACION_PEDIDOS_YA",
                null));
    }
}
