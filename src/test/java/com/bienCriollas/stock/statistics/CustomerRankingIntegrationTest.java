package com.bienCriollas.stock.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

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
        "spring.datasource.url=jdbc:h2:mem:customer-ranking;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@Transactional
class CustomerRankingIntegrationTest {
    private static final String BASE = "/api/v2/estadisticas/clientes-ranking";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 18);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void contractIncludesOnlyDeliveredParticularsAndCountsAnOrderOnceDespiteMultipleDetails() throws Exception {
        long first = sale("  LUCIA   FERNANDEZ ", DATE, "10.10", "ENTREGADO", "PARTICULAR");
        sale("Lucía Fernández", DATE, "20.20", "ENTREGADO", "PARTICULAR");
        sale("Martín González", DATE, "15", "ENTREGADO", "PARTICULAR");
        sale("   ", DATE, "4.70", "ENTREGADO", "PARTICULAR");
        sale("Lucía Fernández", DATE, "1000", "ENTREGADO", "PEDIDOS_YA");
        sale("Lucía Fernández", DATE, "1000", "CANCELADO", "PARTICULAR");
        sale("Lucía Fernández", DATE, "1000", "PREPARADO", "PARTICULAR");
        sale("Lucía Fernández", DATE, "1000", "PENDIENTE", "PARTICULAR");
        sale("Lucía Fernández", DATE.plusDays(7), "1000", "PENDIENTE", "PARTICULAR");
        addDetails(first);
        var before = jdbcTemplate.queryForList("SELECT * FROM pedido ORDER BY id_pedido");

        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.tipo").value("DIA"))
                .andExpect(jsonPath("$.periodo.desde").value(DATE.toString()))
                .andExpect(jsonPath("$.periodo.hasta").value(DATE.toString()))
                .andExpect(jsonPath("$.orden").value("PEDIDOS"))
                .andExpect(jsonPath("$.totalClientes").value(2))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(50))
                .andExpect(jsonPath("$.totalTopClientes").value(45.30))
                .andExpect(jsonPath("$.porcentajeVentasTop").value(90.60))
                .andExpect(jsonPath("$.clientes.length()").value(2))
                .andExpect(jsonPath("$.clientes[0].posicion").value(1))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Lucía Fernández"))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.clientes[0].totalAcumulado").value(30.30))
                .andExpect(jsonPath("$.clientes[0].ticketPromedio").value(15.15))
                .andExpect(jsonPath("$.clientes[0].totalUnidades").value(60))
                .andExpect(jsonPath("$.clientes[1].posicion").value(2))
                .andExpect(jsonPath("$.clientes[1].totalUnidades").value(0));

        assertThat(jdbcTemplate.queryForList("SELECT * FROM pedido ORDER BY id_pedido")).isEqualTo(before);
    }

    @Test
    void dayUsesCommercialDateIncludingHistoricalNullCreatedAtAndExcludesAdjacentDates() throws Exception {
        long historical = sale("Ana", DATE, "10", "ENTREGADO", "PARTICULAR");
        jdbcTemplate.update("UPDATE pedido SET created_at = NULL WHERE id_pedido = ?", historical);
        sale("Ana", DATE, "20", "ENTREGADO", "PARTICULAR");
        sale("Ana", DATE.minusDays(1), "1000", "ENTREGADO", "PARTICULAR");
        sale("Ana", DATE.plusDays(1), "1000", "ENTREGADO", "PARTICULAR");
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(1))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(30))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2));
    }

    @Test
    void sevenDaysAccumulateTheSameCustomerAcrossTheInclusivePeriod() throws Exception {
        sale("Juan Pérez", DATE.minusDays(6), "25", "ENTREGADO", "PARTICULAR");
        sale("JUAN PEREZ", DATE.minusDays(3), "40", "ENTREGADO", "PARTICULAR");
        sale("Juan Pérez", DATE, "35", "ENTREGADO", "PARTICULAR");
        sale("Juan Pérez", DATE.minusDays(7), "1000", "ENTREGADO", "PARTICULAR");
        sale("Juan Pérez", DATE.plusDays(1), "1000", "ENTREGADO", "PARTICULAR");
        mockMvc.perform(get(BASE).param("periodo", "ULTIMOS_7_DIAS").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.desde").value("2026-09-12"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-09-18"))
                .andExpect(jsonPath("$.clientes.length()").value(1))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Juan Pérez"))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(3))
                .andExpect(jsonPath("$.clientes[0].totalAcumulado").value(100))
                .andExpect(jsonPath("$.clientes[0].ticketPromedio").value(33.33));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02", "2024-02", "2026-04", "2026-09", "2026-12"})
    void monthIncludesFirstAndLastDaysAndHandlesLeapYears(String value) throws Exception {
        YearMonth month = YearMonth.parse(value);
        sale("Ana", month.atDay(1), "10", "ENTREGADO", "PARTICULAR");
        sale("ANA", month.atEndOfMonth(), "20", "ENTREGADO", "PARTICULAR");
        sale("Ana", month.atDay(1).minusDays(1), "1000", "ENTREGADO", "PARTICULAR");
        sale("Ana", month.atEndOfMonth().plusDays(1), "1000", "ENTREGADO", "PARTICULAR");
        mockMvc.perform(get(BASE).param("periodo", "MES").param("mes", value).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.tipo").value("MES"))
                .andExpect(jsonPath("$.periodo.desde").value(month.atDay(1).toString()))
                .andExpect(jsonPath("$.periodo.hasta").value(month.atEndOfMonth().toString()))
                .andExpect(jsonPath("$.totalClientes").value(1))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(30))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2));
    }

    @Test
    void defaultTopFivePreservesTotalsAcrossAllCustomers() throws Exception {
        for (int i = 1; i <= 6; i++) {
            sale("Cliente " + i, DATE, Integer.toString(i * 10), "ENTREGADO", "PARTICULAR");
        }
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(6))
                .andExpect(jsonPath("$.clientes.length()").value(5))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Cliente 6"))
                .andExpect(jsonPath("$.clientes[4].posicion").value(5))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(210))
                .andExpect(jsonPath("$.totalTopClientes").value(200))
                .andExpect(jsonPath("$.porcentajeVentasTop").value(95.24));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50, 100})
    void supportsExplicitLimitsWithoutChangingTheTotalCustomerCount(int limit) throws Exception {
        for (int i = 1; i <= 101; i++) {
            sale("Cliente " + i, DATE, Integer.toString(i), "ENTREGADO", "PARTICULAR");
        }
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString())
                .param("limit", Integer.toString(limit)).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(101))
                .andExpect(jsonPath("$.clientes.length()").value(limit))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Cliente 101"))
                .andExpect(jsonPath("$.clientes[" + (limit - 1) + "].posicion").value(limit));
    }

    @Test
    void alwaysPrioritizesMoreOrdersEvenWhenTheFrontendExplicitlyRequestsAmountOrder() throws Exception {
        sale("Ana", DATE, "1", "ENTREGADO", "PARTICULAR");
        sale("Ana", DATE, "2", "ENTREGADO", "PARTICULAR");
        sale("Berta", DATE, "100", "ENTREGADO", "PARTICULAR");
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString())
                .param("limit", "1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden").value("PEDIDOS"))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Ana"))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.totalTopClientes").value(3))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(103))
                .andExpect(jsonPath("$.porcentajeVentasTop").value(2.91));

        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString())
                .param("orden", "IMPORTE").param("limit", "1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden").value("PEDIDOS"))
                .andExpect(jsonPath("$.clientes[0].cliente").value("Ana"))
                .andExpect(jsonPath("$.clientes[0].cantidadPedidos").value(2))
                .andExpect(jsonPath("$.totalTopClientes").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "?periodo=DIA&fecha=2026-09-18", "?periodo=ULTIMOS_7_DIAS&fecha=2026-09-18",
            "?periodo=MES&mes=2026-09", "?periodo=ANIO&anio=2026"
    })
    void guzmanWithTwentyOneOrdersRanksAboveLargeOneOffSalesInEveryPeriod(String query) throws Exception {
        sale("Prestamo Py", DATE, "800000", "ENTREGADO", "PARTICULAR");
        sale("Sigma Y Ut", DATE, "780000", "ENTREGADO", "PARTICULAR");
        for (int i = 0; i < 21; i++) {
            sale("Guzman", DATE, "100", "ENTREGADO", "PARTICULAR");
        }
        for (int i = 0; i < 17; i++) {
            sale("Gustavo Balmaceda", DATE, "100", "ENTREGADO", "PARTICULAR");
        }
        for (int i = 0; i < 10; i++) {
            sale("Lisandro Carbajal", DATE, "100", "ENTREGADO", "PARTICULAR");
        }
        mockMvc.perform(get(BASE + query + "&orden=IMPORTE&limit=5").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden").value("PEDIDOS"))
                .andExpect(jsonPath("$.clientes[*].cliente").value(org.hamcrest.Matchers.contains(
                        "Guzman", "Gustavo Balmaceda", "Lisandro Carbajal", "Prestamo Py", "Sigma Y Ut")))
                .andExpect(jsonPath("$.clientes[*].cantidadPedidos").value(org.hamcrest.Matchers.contains(21, 17, 10, 1, 1)))
                .andExpect(jsonPath("$.clientes[0].posicion").value(1));
    }

    @Test
    void emptyPeriodReturns200AndZeros() throws Exception {
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(0))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(0))
                .andExpect(jsonPath("$.totalTopClientes").value(0))
                .andExpect(jsonPath("$.porcentajeVentasTop").value(0))
                .andExpect(jsonPath("$.clientes").isEmpty());
    }

    @Test
    void anonymousOnlyPeriodHasAnEmptyRankingButKeepsItsParticularSales() throws Exception {
        sale("", DATE, "10", "ENTREGADO", "PARTICULAR");
        sale("   ", DATE, "20", "ENTREGADO", "PARTICULAR");
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(0))
                .andExpect(jsonPath("$.ventasParticularesPeriodo").value(30))
                .andExpect(jsonPath("$.totalTopClientes").value(0))
                .andExpect(jsonPath("$.porcentajeVentasTop").value(0))
                .andExpect(jsonPath("$.clientes").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "?periodo=ANIO&fecha=2026-09-18", "?periodo=DIA", "?periodo=ULTIMOS_7_DIAS", "?periodo=MES",
            "?periodo=DIA&fecha=2026-02-30", "?periodo=DIA&fecha=foo",
            "?periodo=MES&mes=2026-13", "?periodo=MES&mes=foo", "?periodo=MES&mes=2026-9",
            "?periodo=DIA&fecha=2026-09-18&limit=0", "?periodo=DIA&fecha=2026-09-18&limit=-1",
            "?periodo=DIA&fecha=2026-09-18&limit=101", "?periodo=DIA&fecha=2026-09-18&limit=foo",
            "?periodo=DIA&fecha=2026-09-18&orden=CLIENTE"
    })
    void invalidFiltersOrderOrLimitsReturn400(String query) throws Exception {
        mockMvc.perform(get(BASE + query).with(admin())).andExpect(status().isBadRequest());
    }

    @Test
    void endpointRequiresTheExistingStatisticsAdministratorRole() throws Exception {
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE).param("periodo", "DIA").param("fecha", DATE.toString())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLEADO"))))
                .andExpect(status().isForbidden());
    }

    private long sale(String name, LocalDate date, String amount, String state, String type) {
        // Different creation/delivery dates prove that the commercial date is the ranking's source.
        jdbcTemplate.update("""
                INSERT INTO pedido
                    (nombre_cliente, tipo_venta, tipo_pago, monto_efectivo, monto_transferencia,
                     total_pedido, estado, fecha_pedido, fecha_entrega, hora_entrega,
                     created_at, stock_discounted)
                VALUES (?, ?, 'EFECTIVO', ?, 0, ?, ?, ?, ?, ?, ?, false)
                """, name, type, new BigDecimal(amount), new BigDecimal(amount), state, date,
                DATE.plusDays(20), LocalTime.of(22, 0), DATE.minusDays(20).atTime(21, 0));
        return jdbcTemplate.queryForObject("SELECT MAX(id_pedido) FROM pedido", Long.class);
    }

    private void addDetails(long orderId) {
        jdbcTemplate.update("""
                INSERT INTO variedad_empanada
                    (id_variedad, nombre, precio_unitario, precio_media_docena, precio_docena, activo)
                VALUES (99998, 'Carne ranking', 1800, 9500, 18000, 1)
                """);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99998, 48)", orderId);
        jdbcTemplate.update("INSERT INTO pedido_detalle (id_pedido, id_variedad, cantidad) VALUES (?, 99998, 12)", orderId);
    }

    private JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }
}
