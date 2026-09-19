package com.bienCriollas.stock.statistics;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scheduled-order-statistics;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@Transactional
class ScheduledOrderStatisticsIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void deliveredScheduledOrdersBelongToTheirDeliveryDateAcrossTheCommercialSummary() throws Exception {
        long scheduledForToday = order("Programado Hoy", TODAY.minusDays(5), TODAY, "100", "ENTREGADO");
        long ordinaryToday = order("Pedido De Hoy", TODAY, null, "50", "ENTREGADO");
        long deliveredForTomorrow = order("Programado Mañana", TODAY, TODAY.plusDays(1), "1000", "ENTREGADO");
        order("Programado Pendiente", TODAY.minusDays(4), TODAY, "2000", "PENDIENTE");
        addDetails(scheduledForToday, 12);
        addDetails(ordinaryToday, 6);
        addDetails(deliveredForTomorrow, 24);

        mockMvc.perform(get("/api/v2/estadisticas/resumen")
                        .param("periodo", "DIA").param("fecha", TODAY.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosEntregados").value(2))
                .andExpect(jsonPath("$.empanadasVendidas").value(18))
                .andExpect(jsonPath("$.ticketPromedio").value(75))
                .andExpect(jsonPath("$.variedadMasVendida.unidadesVendidas").value(18))
                .andExpect(jsonPath("$.rankingVariedades[0].unidadesVendidas").value(18))
                .andExpect(jsonPath("$.ventasPorDiaSemana.length()").value(1))
                .andExpect(jsonPath("$.ventasPorDiaSemana[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.ventasPorDiaSemana[0].unidadesVendidas").value(18))
                .andExpect(jsonPath("$.ventasPorDiaSemana[0].totalVendido").value(150))
                .andExpect(jsonPath("$.tiposVenta[0].tipoVenta").value("PARTICULAR"))
                .andExpect(jsonPath("$.tiposVenta[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.tiposVenta[0].porcentaje").value(100))
                .andExpect(jsonPath("$.mediosPago[0].medioPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.mediosPago[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.mediosPago[0].porcentaje").value(100));
    }

    @Test
    void deliveredScheduledOrdersBelongToTheirDeliveryDateInTheCustomerRanking() throws Exception {
        order("Cliente Programado", TODAY.minusDays(5), TODAY, "100", "ENTREGADO");
        order("Cliente Programado", TODAY.minusDays(4), TODAY, "50", "ENTREGADO");
        order("Compra Grande Mañana", TODAY, TODAY.plusDays(1), "5000", "ENTREGADO");

        mockMvc.perform(get("/api/v2/estadisticas/clientes-ranking")
                        .param("periodo", "DIA").param("fecha", TODAY.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(1))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(150))
                .andExpect(jsonPath("$.clientes.length()").value(1))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Cliente Programado"))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.clientes[0].totalAcumulado").value(150));
    }

    private long order(String customer, LocalDate creationDate, LocalDate deliveryDate,
            String amount, String state) {
        jdbcTemplate.update("""
                INSERT INTO pedido
                    (nombre_cliente, tipo_venta, tipo_pago, monto_efectivo, monto_transferencia,
                     total_pedido, estado, fecha_pedido, fecha_entrega, created_at, stock_discounted)
                VALUES (?, 'PARTICULAR', 'EFECTIVO', ?, 0, ?, ?, ?, ?, ?, false)
                """, customer, new BigDecimal(amount), new BigDecimal(amount), state,
                creationDate, deliveryDate, LocalDateTime.of(creationDate, java.time.LocalTime.NOON));
        return jdbcTemplate.queryForObject("SELECT MAX(id_pedido) FROM pedido", Long.class);
    }

    private void addDetails(long orderId, int quantity) {
        jdbcTemplate.update("""
                MERGE INTO variedad_empanada
                    (id_variedad, nombre, precio_unitario, precio_media_docena, precio_docena, activo)
                KEY(id_variedad) VALUES (99996, 'Carne programados estadísticas', 1800, 9500, 18000, 1)
                """);
        jdbcTemplate.update(
                "INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99996, ?)",
                orderId, quantity);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }
}
