package com.bienCriollas.stock.statistics;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:peak-hour;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@Transactional
class PeakHourIntegrationTest {

    private static final String BASE = "/api/v2/estadisticas/hora-pico";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void countsOnlyDeliveredOrdersWithCreationTimeAndReturnsTheContract() throws Exception {
        sale(DATE.atTime(11, 30), "100.10", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(12, 29, 59), "200.20", "ENTREGADO", "PEDIDOS_YA");
        sale(DATE.atTime(12, 30), "300.30", "ENTREGADO", "PARTICULAR");
        long orderId = sale(DATE.atTime(21, 42, 15), "400.40", "ENTREGADO", "PEDIDOS_YA");
        sale(DATE.atTime(21, 59, 59), "500.50", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(14, 30), "600.60", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(23, 30), "700.70", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(11, 29, 59), "800.80", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(20, 29, 59), "900.90", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(21, 40), "5000", "PENDIENTE", "PARTICULAR");
        sale(DATE.atTime(21, 40), "6000", "PREPARADO", "PEDIDOS_YA");
        sale(DATE.atTime(21, 40), "7000", "CANCELADO", "PARTICULAR");
        sale(null, "8000", "ENTREGADO", "PARTICULAR");
        addMultipleDetails(orderId);

        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.tipo").value("DIA"))
                .andExpect(jsonPath("$.periodo.desde").value("2026-09-18"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-09-18"))
                .andExpect(jsonPath("$.totalPedidosAnalizados").value(9))
                .andExpect(jsonPath("$.totalMontoVendido").value(4504.50))
                .andExpect(jsonPath("$.horaPico.inicio").value("21:30"))
                .andExpect(jsonPath("$.horaPico.fin").value("22:00"))
                .andExpect(jsonPath("$.horaPico.pedidos").value(2))
                .andExpect(jsonPath("$.horaPico.montoVendido").value(900.90))
                .andExpect(jsonPath("$.horaPico.porcentajeDelTotal").value(22.22))
                .andExpect(jsonPath("$.horaPico.turno").value("NOCHE"))
                .andExpect(jsonPath("$.turnos.mediodia.desde").value("11:30"))
                .andExpect(jsonPath("$.turnos.mediodia.hasta").value("14:30"))
                .andExpect(jsonPath("$.turnos.mediodia.totalPedidos").value(3))
                .andExpect(jsonPath("$.turnos.mediodia.totalMontoVendido").value(600.60))
                .andExpect(jsonPath("$.turnos.mediodia.franjas.length()").value(6))
                .andExpect(jsonPath("$.turnos.mediodia.franjas[5].inicio").value("14:00"))
                .andExpect(jsonPath("$.turnos.mediodia.franjas[5].fin").value("14:30"))
                .andExpect(jsonPath("$.turnos.mediodia.franjas[5].pedidos").value(0))
                .andExpect(jsonPath("$.turnos.mediodia.franjas[5].montoVendido").value(0))
                .andExpect(jsonPath("$.turnos.noche.desde").value("20:30"))
                .andExpect(jsonPath("$.turnos.noche.hasta").value("23:30"))
                .andExpect(jsonPath("$.turnos.noche.totalPedidos").value(2))
                .andExpect(jsonPath("$.turnos.noche.totalMontoVendido").value(900.90))
                .andExpect(jsonPath("$.turnos.noche.franjas.length()").value(6))
                .andExpect(jsonPath("$.turnos.noche.franjas[2].porcentajeDelTotal").value(22.22))
                .andExpect(jsonPath("$.pedidosFueraDeHorario").value(4))
                .andExpect(jsonPath("$.montoFueraDeHorario").value(3003.00));
    }

    @Test
    void dayIncludesMidnightAndTheLastSecondButExcludesAdjacentDays() throws Exception {
        sale(DATE.atStartOfDay(), "10", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(23, 59, 59), "20", "ENTREGADO", "PARTICULAR");
        sale(DATE.atStartOfDay().minusSeconds(1), "1000", "ENTREGADO", "PARTICULAR");
        sale(DATE.plusDays(1).atStartOfDay(), "1000", "ENTREGADO", "PARTICULAR");

        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidosAnalizados").value(2))
                .andExpect(jsonPath("$.totalMontoVendido").value(30))
                .andExpect(jsonPath("$.pedidosFueraDeHorario").value(2))
                .andExpect(jsonPath("$.horaPico").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void sevenDaysIncludeSelectedDateAndPreviousSixAndAccumulateTheSameSlot() throws Exception {
        LocalDate firstDay = DATE.minusDays(6);
        sale(firstDay.atTime(21, 30), "11", "ENTREGADO", "PARTICULAR");
        sale(DATE.atTime(21, 59), "22", "ENTREGADO", "PEDIDOS_YA");
        sale(DATE.minusDays(3).atTime(21, 35), "33", "ENTREGADO", "PARTICULAR");
        sale(firstDay.minusDays(1).atTime(21, 40), "1000", "ENTREGADO", "PARTICULAR");
        sale(DATE.plusDays(1).atStartOfDay(), "1000", "ENTREGADO", "PARTICULAR");

        mockMvc.perform(get(BASE).param("periodo", "ULTIMOS_7_DIAS").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.desde").value("2026-09-12"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-09-18"))
                .andExpect(jsonPath("$.totalPedidosAnalizados").value(3))
                .andExpect(jsonPath("$.horaPico.inicio").value("21:30"))
                .andExpect(jsonPath("$.horaPico.pedidos").value(3))
                .andExpect(jsonPath("$.horaPico.montoVendido").value(66))
                .andExpect(jsonPath("$.horaPico.porcentajeDelTotal").value(100))
                .andExpect(jsonPath("$.turnos.noche.franjas[2].pedidos").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02", "2024-02", "2026-04", "2026-09", "2026-12"})
    void monthHandlesAllMonthLengthsAndYearRollover(String value) throws Exception {
        YearMonth month = YearMonth.parse(value);
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        sale(firstDay.atStartOfDay(), "5", "ENTREGADO", "PARTICULAR");
        sale(firstDay.atTime(21, 30), "10", "ENTREGADO", "PARTICULAR");
        sale(lastDay.atTime(21, 40), "20", "ENTREGADO", "PEDIDOS_YA");
        sale(firstDay.atStartOfDay().minusSeconds(1), "1000", "ENTREGADO", "PARTICULAR");
        sale(lastDay.plusDays(1).atStartOfDay(), "1000", "ENTREGADO", "PARTICULAR");

        mockMvc.perform(get(BASE).param("periodo", "MES").param("mes", value).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.desde").value(firstDay.toString()))
                .andExpect(jsonPath("$.periodo.hasta").value(lastDay.toString()))
                .andExpect(jsonPath("$.totalPedidosAnalizados").value(3))
                .andExpect(jsonPath("$.totalMontoVendido").value(35))
                .andExpect(jsonPath("$.horaPico.inicio").value("21:30"))
                .andExpect(jsonPath("$.horaPico.pedidos").value(2))
                .andExpect(jsonPath("$.horaPico.porcentajeDelTotal").value(66.67));
    }

    @Test
    void emptyPeriodReturns200WithNullPeakAndTwelveZeroSlots() throws Exception {
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaPico").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.totalPedidosAnalizados").value(0))
                .andExpect(jsonPath("$.totalMontoVendido").value(0))
                .andExpect(jsonPath("$.pedidosFueraDeHorario").value(0))
                .andExpect(jsonPath("$.montoFueraDeHorario").value(0))
                .andExpect(jsonPath("$.turnos.mediodia.franjas.length()").value(6))
                .andExpect(jsonPath("$.turnos.noche.franjas.length()").value(6))
                .andExpect(jsonPath("$.turnos.*.franjas[*].pedidos").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(0))))
                .andExpect(jsonPath("$.turnos.*.franjas[*].montoVendido").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(0))))
                .andExpect(jsonPath("$.turnos.*.franjas[*].porcentajeDelTotal").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(0))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "?periodo=ANIO&fecha=2026-09-18", "?periodo=DIA", "?periodo=ULTIMOS_7_DIAS", "?periodo=MES",
            "?periodo=DIA&fecha=2026-02-30", "?periodo=DIA&fecha=foo",
            "?periodo=MES&mes=2026-13", "?periodo=MES&mes=foo", "?periodo=MES&mes=2026-9"
    })
    void missingOrInvalidFiltersReturn400(String query) throws Exception {
        mockMvc.perform(get(BASE + query).with(admin())).andExpect(status().isBadRequest());
    }

    @Test
    void preservesTheArgentinaTimeEvenWhenTheServerUsesUtc() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            sale(DATE.atTime(21, 45), "18000", "ENTREGADO", "PARTICULAR");
            mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horaPico.inicio").value("21:30"))
                    .andExpect(jsonPath("$.horaPico.turno").value("NOCHE"));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void reusesStatisticsAdministratorPermissions() throws Exception {
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLEADO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void existingSummaryKeepsItsInclusiveStartAndExclusiveEnd() throws Exception {
        long included = sale(DATE.atTime(12, 0), "100", "ENTREGADO", "PARTICULAR");
        long excluded = sale(DATE.atTime(13, 0), "1000", "ENTREGADO", "PARTICULAR");
        jdbcTemplate.update("UPDATE pedido SET fecha_pedido = ? WHERE id_pedido = ?", DATE, included);
        jdbcTemplate.update("UPDATE pedido SET fecha_pedido = ? WHERE id_pedido = ?", DATE.plusDays(1), excluded);

        mockMvc.perform(get("/api/v2/estadisticas/resumen")
                .param("desde", DATE.toString()).param("hasta", DATE.plusDays(1).toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosEntregados").value(1))
                .andExpect(jsonPath("$.ticketPromedio").value(100));
    }

    private long sale(LocalDateTime createdAt, String amount, String state, String saleType) {
        // Fechas comerciales y de entrega distintas demuestran que solo se consulta created_at.
        jdbcTemplate.update("""
                INSERT INTO pedido
                    (nombre_cliente, tipo_venta, tipo_pago, monto_efectivo, monto_transferencia,
                     total_pedido, estado, fecha_pedido, fecha_entrega, hora_entrega,
                     created_at, stock_discounted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "Cliente", saleType, "EFECTIVO", new BigDecimal(amount), BigDecimal.ZERO,
                new BigDecimal(amount), state, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 3),
                LocalTime.of(22, 0), createdAt, false);
        return jdbcTemplate.queryForObject("SELECT MAX(id_pedido) FROM pedido", Long.class);
    }

    private void addMultipleDetails(long id) {
        jdbcTemplate.update("""
                INSERT INTO variedad_empanada
                    (id_variedad, nombre, precio_unitario, precio_media_docena, precio_docena, activo)
                VALUES (99999, 'Carne hora pico', 1800, 9500, 18000, 1)
                """);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99999, 48)", id);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99999, 12)", id);
    }

    private JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }
}
