package com.bienCriollas.stock.statistics;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        "spring.datasource.url=jdbc:h2:mem:annual-statistics;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@Transactional
class AnnualStatisticsIntegrationTest {
    private static final String BASE = "/api/v2/estadisticas";
    private static final List<String> ENDPOINTS = List.of("/resumen", "/hora-pico", "/clientes-ranking");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(ints = {2024, 2026})
    void fullYearFiltersEverySummaryMetricAndAccumulatesPeakSlotsAndCustomers(int year) throws Exception {
        LocalDate first = LocalDate.of(year, 1, 1);
        LocalDate last = LocalDate.of(year, 12, 31);
        LocalDate februaryEnd = LocalDate.of(year, 2, 1).plusMonths(1).minusDays(1);
        long firstOrder = sale("Ana", first.atStartOfDay(), "10", "ENTREGADO");
        long lastOrder = sale("Berta", last.atTime(21, 45), "20", "ENTREGADO");
        long februaryOrder = sale("ANA", februaryEnd.atTime(21, 45), "30", "ENTREGADO");
        sale("Fuera", first.atStartOfDay().minusSeconds(1), "1000", "ENTREGADO");
        sale("Fuera", last.plusDays(1).atStartOfDay(), "1000", "ENTREGADO");
        sale("Ana", first.atTime(12, 0), "1000", "CANCELADO");
        sale("Ana", first.atTime(12, 0), "1000", "PENDIENTE");
        sale("Ana", first.atTime(12, 0), "1000", "PREPARADO");
        addDetailsAndWaste(firstOrder, lastOrder, februaryOrder, first, last);

        mockMvc.perform(get(BASE + "/resumen").param("periodo", "ANIO").param("anio", Integer.toString(year)).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosEntregados").value(3))
                .andExpect(jsonPath("$.empanadasVendidas").value(27))
                .andExpect(jsonPath("$.ticketPromedio").value(20))
                .andExpect(jsonPath("$.variedadMasVendida.unidadesVendidas").value(27))
                .andExpect(jsonPath("$.rankingVariedades[0].unidadesVendidas").value(27))
                .andExpect(jsonPath("$.ventasPorDiaSemana[*].cantidadPedidos").value(
                        org.hamcrest.Matchers.containsInAnyOrder(year == 2024 ? new Integer[]{1, 1, 1} : new Integer[]{2, 1})))
                .andExpect(jsonPath("$.ventasPorDiaSemana[*].totalVendido").value(
                        org.hamcrest.Matchers.containsInAnyOrder(year == 2024 ? new Double[]{10.0, 20.0, 30.0} : new Double[]{30.0, 30.0})))
                .andExpect(jsonPath("$.tiposVenta[0].cantidadPedidos").value(3))
                .andExpect(jsonPath("$.mediosPago[0].cantidadPedidos").value(3))
                .andExpect(jsonPath("$.mermasPorVariedad[0].unidadesPerdidas").value(2));

        mockMvc.perform(get(BASE + "/hora-pico").param("periodo", "ANIO").param("anio", Integer.toString(year)).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.tipo").value("ANIO"))
                .andExpect(jsonPath("$.periodo.desde").value(first.toString()))
                .andExpect(jsonPath("$.periodo.hasta").value(last.toString()))
                .andExpect(jsonPath("$.totalPedidosAnalizados").value(3))
                .andExpect(jsonPath("$.totalMontoVendido").value(60))
                .andExpect(jsonPath("$.horaPico.inicio").value("21:30"))
                .andExpect(jsonPath("$.horaPico.pedidos").value(2))
                .andExpect(jsonPath("$.horaPico.montoVendido").value(50))
                .andExpect(jsonPath("$.horaPico.porcentajeDelTotal").value(66.67));

        mockMvc.perform(get(BASE + "/clientes-ranking").param("periodo", "ANIO").param("anio", Integer.toString(year))
                .param("limit", "1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.tipo").value("ANIO"))
                .andExpect(jsonPath("$.periodo.desde").value(first.toString()))
                .andExpect(jsonPath("$.periodo.hasta").value(last.toString()))
                .andExpect(jsonPath("$.totalClientes").value(2))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(60))
                .andExpect(jsonPath("$.totalTopClientes").value(40))
                .andExpect(jsonPath("$.porcentajeVentasTop").value(66.67))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Ana"))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.clientes[0].ticketPromedio").value(20))
                .andExpect(jsonPath("$.clientes[0].totalUnidades").value(15));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "&anio=foo", "&anio=2026.5", "&anio=-1", "&anio=0", "&anio=999", "&anio=9999", "&anio=10000"})
    void invalidOrMissingYearReturns400AtEveryEndpoint(String yearQuery) throws Exception {
        for (String endpoint : ENDPOINTS) {
            mockMvc.perform(get(BASE + endpoint + "?periodo=ANIO" + yearQuery).with(admin()))
                    .andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 9998})
    void supportedYearBoundsReturn200AtEveryEndpoint(int year) throws Exception {
        for (String endpoint : ENDPOINTS) {
            mockMvc.perform(get(BASE + endpoint).param("periodo", "ANIO").param("anio", Integer.toString(year)).with(admin()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void emptyYearReturnsAnEmptySummaryPeakAndCustomerRanking() throws Exception {
        mockMvc.perform(get(BASE + "/resumen?periodo=ANIO&anio=2026").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedidosEntregados").value(0));
        mockMvc.perform(get(BASE + "/hora-pico?periodo=ANIO&anio=2026").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPedidosAnalizados").value(0))
                .andExpect(jsonPath("$.horaPico").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(get(BASE + "/clientes-ranking?periodo=ANIO&anio=2026").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalClientes").value(0))
                .andExpect(jsonPath("$.clientes").isEmpty());
    }

    @Test
    void summaryKeepsItsLegacyRangeAndSupportsTheExistingGlobalPeriods() throws Exception {
        sale("Ana", LocalDateTime.of(2026, 9, 18, 12, 0), "10", "ENTREGADO");
        sale("Ana", LocalDateTime.of(2026, 9, 19, 12, 0), "20", "ENTREGADO");
        for (String query : List.of(
                "?desde=2026-09-18&hasta=2026-09-19",
                "?periodo=DIA&fecha=2026-09-18",
                "?periodo=ULTIMOS_7_DIAS&fecha=2026-09-18")) {
            mockMvc.perform(get(BASE + "/resumen" + query).with(admin()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.pedidosEntregados").value(1));
        }
        mockMvc.perform(get(BASE + "/resumen?periodo=MES&mes=2026-09").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedidosEntregados").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "?desde=2026-01-01", "?hasta=2027-01-01", "?desde=2026-01-01&hasta=2026-01-01",
            "?anio=2026", "?periodo=DIA", "?periodo=ULTIMOS_7_DIAS", "?periodo=MES",
            "?periodo=ANIO&anio=2026&desde=2026-01-01",
            "?periodo=ANIO&anio=2026&hasta=2027-01-01"
    })
    void summaryRejectsMissingOrAmbiguousFilters(String query) throws Exception {
        mockMvc.perform(get(BASE + "/resumen" + query).with(admin())).andExpect(status().isBadRequest());
    }

    private long sale(String name, LocalDateTime createdAt, String amount, String state) {
        jdbcTemplate.update("""
                INSERT INTO pedido
                    (nombre_cliente, tipo_venta, tipo_pago, monto_efectivo, monto_transferencia,
                     total_pedido, estado, fecha_pedido, fecha_entrega, created_at, stock_discounted)
                VALUES (?, 'PARTICULAR', 'EFECTIVO', ?, 0, ?, ?, ?, ?, ?, false)
                """, name, new BigDecimal(amount), new BigDecimal(amount), state,
                createdAt.toLocalDate(), createdAt.toLocalDate().plusDays(2), createdAt);
        return jdbcTemplate.queryForObject("SELECT MAX(id_pedido) FROM pedido", Long.class);
    }

    private void addDetailsAndWaste(long firstId, long lastId, long februaryId, LocalDate first, LocalDate last) {
        jdbcTemplate.update("""
                INSERT INTO variedad_empanada
                    (id_variedad, nombre, precio_unitario, precio_media_docena, precio_docena, activo)
                VALUES (99997, 'Carne anual', 1800, 9500, 18000, 1)
                """);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99997, 6)", firstId);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99997, 6)", firstId);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99997, 12)", lastId);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99997, 3)", februaryId);
        for (LocalDateTime timestamp : List.of(first.atStartOfDay(), last.atTime(23, 59, 59),
                first.atStartOfDay().minusSeconds(1), last.plusDays(1).atStartOfDay())) {
            jdbcTemplate.update("INSERT INTO merma_empanada (id_variedad, cantidad, fecha_registro) VALUES (99997, 1, ?)", timestamp);
        }
    }

    private JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }
}
