package com.bienCriollas.stock.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-created-at;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@Transactional
class OrderCreatedAtIntegrationTest {

    private static final String BASE = "/api/v2/pedido";
    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private Long varietyId;

    @BeforeEach
    void prepareStock() {
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .name("Carne createdAt")
                .unitPrice(new BigDecimal("1800"))
                .halfDozenPrice(new BigDecimal("9500"))
                .dozenPrice(new BigDecimal("18000"))
                .active(1)
                .build();
        entityManager.persist(variety);
        varietyId = variety.getVarietyId();
        entityManager.persist(Stock.builder()
                .varietyId(varietyId)
                .productionDate(today())
                .totalStock(100)
                .availableStock(100)
                .active(1)
                .build());
        entityManager.flush();
    }

    @ParameterizedTest
    @CsvSource({"PARTICULAR,false", "PARTICULAR,true", "PEDIDOS_YA,false", "PEDIDOS_YA,true"})
    void newOrdersUseArgentinaTimeRegardlessOfServerTimezoneOrClientTimestamp(
            String saleType, boolean scheduled) throws Exception {
        TimeZone originalTimezone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LocalDate deliveryDate = scheduled ? today().plusDays(2) : null;
            LocalDateTime before = argentinaNow();
            JsonNode response = createOrder(saleType, deliveryDate);
            LocalDateTime after = argentinaNow();

            LocalDateTime createdAt = LocalDateTime.parse(response.get("createdAt").asString());
            assertThat(createdAt).isBetween(before, after);
            assertThat(createdAt.getNano()).isZero();
            assertThat(response.get("fechaPedido").asString()).isEqualTo(today().toString());
            assertThat(response.get("horaEntrega").asString()).isEqualTo("21:00:00");
            assertThat(response.get("scheduled").asBoolean()).isEqualTo(scheduled);
            assertThat(response.get("pagado").asBoolean()).isFalse();
            if (scheduled) {
                assertThat(response.get("fechaEntrega").asString()).isEqualTo(deliveryDate.toString());
                assertThat(createdAt.toLocalDate()).isBefore(deliveryDate);
            } else {
                assertThat(response.get("fechaEntrega").isNull()).isTrue();
            }

            assertThat(reload(response.get("idPedido").asLong()).getCreatedAt()).isEqualTo(createdAt);
        } finally {
            TimeZone.setDefault(originalTimezone);
        }
    }

    @Test
    void editingAndPreparingAndDeliveringPreserveThePersistedTimestamp() throws Exception {
        JsonNode created = createOrder("PARTICULAR", today().plusDays(2));
        long id = created.get("idPedido").asLong();
        LocalDateTime original = reload(id).getCreatedAt();

        JsonNode updated = jsonMapper.readTree(mockMvc.perform(put(BASE + "/actualizar/{id}", id)
                .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(request("PEDIDOS_YA", today().plusDays(3))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(updated.get("createdAt").asString()).isEqualTo(created.get("createdAt").asString());
        assertThat(reload(id).getCreatedAt()).isEqualTo(original);
        for (String state : new String[]{"PREPARADO", "ENTREGADO"}) {
            mockMvc.perform(put(BASE + "/actualizar-estado/{id}/{estado}", id, state).with(jwt()))
                    .andExpect(status().isOk());
            assertThat(reload(id).getCreatedAt()).isEqualTo(original);
        }
    }

    @Test
    void cancellingPreservesThePersistedTimestamp() throws Exception {
        JsonNode created = createOrder("PEDIDOS_YA", today().plusDays(2));
        long id = created.get("idPedido").asLong();
        LocalDateTime original = reload(id).getCreatedAt();

        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/CANCELADO", id).with(jwt()))
                .andExpect(status().isOk());

        Order cancelled = reload(id);
        assertThat(cancelled.getCreatedAt()).isEqualTo(original);
        assertThat(cancelled.getCashAmount()).isEqualByComparingTo("0.00");
        assertThat(cancelled.getTransferAmount()).isEqualByComparingTo("0.00");
        assertThat(cancelled.getOrderTotal()).isEqualByComparingTo("0.00");
        assertThat(cancelled.isPagado()).isFalse();
    }

    @Test
    void paidStatusFlowsThroughCreationPreparationDeliveryPatchAndCancellation() throws Exception {
        JsonNode prepaid = jsonMapper.readTree(mockMvc.perform(post(BASE + "/crear").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("PARTICULAR", null, true)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long prepaidId = prepaid.get("idPedido").asLong();
        assertThat(prepaid.get("pagado").asBoolean()).isTrue();

        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/PREPARADO", prepaidId).with(jwt()))
                .andExpect(status().isOk());
        assertThat(reload(prepaidId).isPagado()).isTrue();
        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/CANCELADO", prepaidId).with(jwt()))
                .andExpect(status().isOk());
        assertThat(reload(prepaidId).isPagado()).isTrue();

        JsonNode payOnPickup = createOrder("PARTICULAR", null);
        long payOnPickupId = payOnPickup.get("idPedido").asLong();
        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/PREPARADO", payOnPickupId).with(jwt()))
                .andExpect(status().isOk());
        assertThat(reload(payOnPickupId).isPagado()).isFalse();

        JsonNode paidByPatch = jsonMapper.readTree(mockMvc.perform(
                        patch(BASE + "/{id}/pago", payOnPickupId).with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagado\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(paidByPatch.get("pagado").asBoolean()).isTrue();

        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/ENTREGADO", payOnPickupId).with(jwt()))
                .andExpect(status().isOk());
        assertThat(reload(payOnPickupId).isPagado()).isTrue();
        mockMvc.perform(patch(BASE + "/{id}/pago", payOnPickupId).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagado\":false}"))
                .andExpect(status().isConflict());
        assertThat(reload(payOnPickupId).isPagado()).isTrue();
    }

    @Test
    void deliveryEtaSupportsCreationEditingPatchRemovalValidationAndStateChanges() throws Exception {
        LocalDateTime beforeCreation = argentinaNow().plusMinutes(31);
        JsonNode created = jsonMapper.readTree(mockMvc.perform(post(BASE + "/crear").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithEta("PEDIDOS_YA", 31)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        LocalDateTime afterCreation = argentinaNow().plusMinutes(31);
        long id = created.get("idPedido").asLong();
        LocalDateTime createdEta = LocalDateTime.parse(
                created.get("fechaHoraEstimadaDelivery").asString());
        assertThat(createdEta).isBetween(beforeCreation, afterCreation);
        assertThat(reload(id).getFechaHoraEstimadaDelivery()).isEqualTo(createdEta);

        JsonNode listed = readOrders(BASE + "/por-fecha/" + today());
        assertThat(listed.get(0).get("fechaHoraEstimadaDelivery").asString())
                .isEqualTo(createdEta.toString());

        JsonNode legacyEdit = jsonMapper.readTree(mockMvc.perform(put(BASE + "/actualizar/{id}", id)
                .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(request("PEDIDOS_YA", null)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(legacyEdit.get("fechaHoraEstimadaDelivery").asString())
                .isEqualTo(createdEta.toString());

        LocalDateTime beforeEdit = argentinaNow().plusMinutes(40);
        JsonNode edited = jsonMapper.readTree(mockMvc.perform(put(BASE + "/actualizar/{id}", id)
                .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(requestWithEta("PEDIDOS_YA", 40)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(LocalDateTime.parse(edited.get("fechaHoraEstimadaDelivery").asString()))
                .isBetween(beforeEdit, argentinaNow().plusMinutes(40));

        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/PREPARADO", id).with(jwt()))
                .andExpect(status().isOk());
        LocalDateTime etaBeforePatch = reload(id).getFechaHoraEstimadaDelivery();
        assertThat(etaBeforePatch).isNotNull();

        LocalDateTime beforePatch = argentinaNow().plusMinutes(20);
        JsonNode patched = jsonMapper.readTree(mockMvc.perform(
                        patch(BASE + "/{id}/eta-delivery", id).with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"minutos\":20}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(patched.get("estadoPedido").asString()).isEqualTo("PREPARADO");
        assertThat(patched.get("pagado").asBoolean()).isFalse();
        assertThat(LocalDateTime.parse(patched.get("fechaHoraEstimadaDelivery").asString()))
                .isBetween(beforePatch, argentinaNow().plusMinutes(20));

        mockMvc.perform(patch(BASE + "/{id}/eta-delivery", id).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutos\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/{id}/eta-delivery", id).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutos\":-5}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/ENTREGADO", id).with(jwt()))
                .andExpect(status().isOk());
        assertThat(reload(id).getFechaHoraEstimadaDelivery()).isNotNull();

        JsonNode withoutEta = jsonMapper.readTree(mockMvc.perform(
                        patch(BASE + "/{id}/eta-delivery", id).with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"minutos\":null}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(withoutEta.get("fechaHoraEstimadaDelivery").isNull()).isTrue();

        JsonNode particular = createOrder("PARTICULAR", null);
        mockMvc.perform(patch(BASE + "/{id}/eta-delivery", particular.get("idPedido").asLong())
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutos\":20}"))
                .andExpect(status().isConflict());
    }

    @Test
    void historicalNullIsReturnedAndStaysNullAfterEditingOrChangingState() throws Exception {
        JsonNode created = createOrder("PARTICULAR", null);
        long id = created.get("idPedido").asLong();
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE pedido SET created_at = NULL WHERE id_pedido = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.clear();

        JsonNode listed = readOrders(BASE + "/por-fecha/" + today());
        assertThat(listed.get(0).has("createdAt")).isTrue();
        assertThat(listed.get(0).get("createdAt").isNull()).isTrue();

        JsonNode updated = jsonMapper.readTree(mockMvc.perform(put(BASE + "/actualizar/{id}", id)
                .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                .content(request("PARTICULAR", null)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(updated.get("createdAt").isNull()).isTrue();

        mockMvc.perform(put(BASE + "/actualizar-estado/{id}/PREPARADO", id).with(jwt()))
                .andExpect(status().isOk());
        assertThat(reload(id).getCreatedAt()).isNull();
    }

    @Test
    void existingListingsIncludeTheOriginalTimestamp() throws Exception {
        JsonNode current = createOrder("PARTICULAR", null);
        LocalDate futureDate = today().plusDays(2);
        JsonNode scheduled = createOrder("PARTICULAR", futureDate);

        for (String url : new String[]{BASE + "/pedido-estado/PENDIENTE", BASE + "/paginado?estado=PENDIENTE"}) {
            assertThat(readOrders(url).get("content").get(0).get("createdAt").asString())
                    .isEqualTo(current.get("createdAt").asString());
        }
        assertThat(readOrders(BASE + "/por-fecha/" + today()).get(0).get("createdAt").asString())
                .isEqualTo(current.get("createdAt").asString());
        for (String url : new String[]{BASE + "/programados", BASE + "/programados?fecha=" + futureDate}) {
            assertThat(readOrders(url).get(0).get("createdAt").asString())
                    .isEqualTo(scheduled.get("createdAt").asString());
        }
    }

    private JsonNode createOrder(String saleType, LocalDate deliveryDate) throws Exception {
        return jsonMapper.readTree(mockMvc.perform(post(BASE + "/crear").with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content(request(saleType, deliveryDate)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode readOrders(String url) throws Exception {
        return jsonMapper.readTree(mockMvc.perform(get(url).with(jwt()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String request(String saleType, LocalDate deliveryDate) {
        // Un cliente no puede controlar createdAt aunque intente enviarlo en el JSON.
        return """
                {
                    "cliente": "Julieta Vargas", "tipoVenta": "%s", "tipoPago": "EFECTIVO",
                    "numeroPedidoPedidosYa": "PYA-created-at", "horaEntrega": "21:00:00",
                    "totalPedido": 18000, "detalles": [{"idVariedad": %d, "cantidad": 12}],
                    "fechaEntrega": %s, "createdAt": "2000-01-01T00:00:00"
                }
                """.formatted(saleType, varietyId, deliveryDate == null ? "null" : "\"" + deliveryDate + "\"");
    }

    private String request(String saleType, LocalDate deliveryDate, boolean pagado) {
        return """
                {
                    "cliente": "Julieta Vargas", "tipoVenta": "%s", "tipoPago": "EFECTIVO",
                    "numeroPedidoPedidosYa": "PYA-payment", "horaEntrega": "21:00:00",
                    "totalPedido": 18000, "detalles": [{"idVariedad": %d, "cantidad": 12}],
                    "fechaEntrega": %s, "pagado": %s
                }
                """.formatted(saleType, varietyId,
                        deliveryDate == null ? "null" : "\"" + deliveryDate + "\"", pagado);
    }

    private String requestWithEta(String saleType, Integer minutes) {
        return """
                {
                    "cliente": "Carlos Castro", "tipoVenta": "%s", "tipoPago": "TRANSFERENCIA",
                    "numeroPedidoPedidosYa": "PYA-eta", "horaEntrega": null,
                    "totalPedido": 18000, "detalles": [{"idVariedad": %d, "cantidad": 12}],
                    "fechaEntrega": null, "pagado": false,
                    "tiempoEstimadoDeliveryMinutos": %s
                }
                """.formatted(saleType, varietyId, minutes == null ? "null" : minutes);
    }

    private Order reload(long id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Order.class, id);
    }

    private LocalDate today() {
        return LocalDate.now(ARGENTINA_ZONE);
    }

    private LocalDateTime argentinaNow() {
        return LocalDateTime.now(ARGENTINA_ZONE).truncatedTo(ChronoUnit.SECONDS);
    }
}
