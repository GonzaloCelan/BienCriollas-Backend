package com.bienCriollas.stock.production.process;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bienCriollas.stock.production.process.dto.*;
import com.bienCriollas.stock.production.process.enums.ProcessTimeType;
import com.bienCriollas.stock.production.process.exception.*;
import com.bienCriollas.stock.production.process.interfaces.IProductionProcessService;
import com.bienCriollas.stock.production.process.repository.ProductionProcessRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:processes-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.hikari.maximum-pool-size=16"
})
@AutoConfigureMockMvc
class ProductionProcessIntegrationTest {

    private static final String BASE = "/api/v1/processes";
    private static final PageRequest PAGE = PageRequest.of(0, 20);

    @Autowired private IProductionProcessService service;
    @Autowired private ProductionProcessRepository processRepository;
    @Autowired private EmpanadaVarietyRepository varietyRepository;
    @Autowired private MockMvc mockMvc;

    private EmpanadaVariety carne;

    @BeforeEach
    void setUp() {
        processRepository.deleteAll();
        varietyRepository.deleteAll();
        carne = variety("Carne");
    }

    @Test
    void createsOrderedStandardProcessAndCalculatesSummary() {
        ProductionProcessResponseDTO result = service.createProcess(request(
                carne.getVarietyId(), 100, "  Proceso estándar  ",
                step("  Preparar verduras  ", 20, 1, ProcessTimeType.ACTIVE),
                step("Enfriar relleno", 60, 0, ProcessTimeType.WAITING),
                step("Armar empanadas", 50, 2, ProcessTimeType.ACTIVE)));

        assertThat(result.version()).isEqualTo(1);
        assertThat(result.active()).isTrue();
        assertThat(result.referenceYieldUnits()).isEqualTo(100);
        assertThat(result.notes()).isEqualTo("Proceso estándar");
        assertThat(result.stepCount()).isEqualTo(3);
        assertThat(result.totalEstimatedMinutes()).isEqualTo(130);
        assertThat(result.activeMinutes()).isEqualTo(70);
        assertThat(result.waitingMinutes()).isEqualTo(60);
        assertThat(result.estimatedPersonMinutes()).isEqualTo(120);
        assertThat(result.estimatedPersonHours()).isEqualByComparingTo("2.00");
        assertThat(result.steps()).extracting(ProductionProcessStepResponseDTO::stepOrder)
                .containsExactly(1, 2, 3);
        assertThat(result.steps()).extracting(ProductionProcessStepResponseDTO::name)
                .containsExactly("Preparar verduras", "Enfriar relleno", "Armar empanadas");
        assertThat(result.steps()).extracting(
                ProductionProcessStepResponseDTO::estimatedPersonMinutes)
                .containsExactly(20, 0, 100);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isEqualTo(result.createdAt());
    }

    @Test
    void createsImmutableVersionsAndKeepsOneCurrentProcess() {
        ProductionProcessResponseDTO first = createProcess();
        ProductionProcessResponseDTO second = service.createNewVersion(first.id(),
                versionRequest(120, "v2",
                        step("Preparar", 25, 1, ProcessTimeType.ACTIVE),
                        step("Porcionar", 15, 1, ProcessTimeType.ACTIVE)));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.version()).isEqualTo(2);
        assertThat(second.active()).isTrue();
        assertThat(service.getProcessById(first.id()).active()).isFalse();
        assertThat(service.getProcessById(first.id()).referenceYieldUnits()).isEqualTo(100);
        assertThat(service.getProcessById(first.id()).steps())
                .extracting(ProductionProcessStepResponseDTO::name)
                .containsExactly("Preparar", "Enfriar", "Armar");
        assertThat(service.getProcessHistory(carne.getVarietyId()))
                .extracting(ProductionProcessResponseDTO::version)
                .containsExactly(2, 1);
        assertThat(processRepository.findByActiveTrue(PAGE).getTotalElements()).isEqualTo(1);
    }

    @Test
    void canVersionFromHistoricalIdAndAlwaysUsesLatestVersion() {
        ProductionProcessResponseDTO first = createProcess();
        ProductionProcessResponseDTO second = service.createNewVersion(first.id(),
                versionRequest(100, "v2", step("Preparar", 20, 1, ProcessTimeType.ACTIVE)));
        ProductionProcessResponseDTO third = service.createNewVersion(first.id(),
                versionRequest(100, "v3", step("Preparar", 18, 1, ProcessTimeType.ACTIVE)));

        assertThat(second.version()).isEqualTo(2);
        assertThat(third.version()).isEqualTo(3);
        assertThat(service.getActiveProcessByVariety(carne.getVarietyId()).id())
                .isEqualTo(third.id());
        assertThat(service.getProcessHistory(carne.getVarietyId()))
                .extracting(ProductionProcessResponseDTO::active)
                .containsExactly(true, false, false);
    }

    @Test
    void validatesActiveAndWaitingPeopleRules() {
        assertThatThrownBy(() -> service.createProcess(request(
                carne.getVarietyId(), 100, null,
                step("Armar empanadas", 50, 0, ProcessTimeType.ACTIVE))))
                .isInstanceOf(InvalidProcessStepException.class)
                .hasMessage("El paso 'Armar empanadas' es de trabajo activo "
                        + "y requiere al menos una persona.");

        ProductionProcessResponseDTO waiting = service.createProcess(request(
                carne.getVarietyId(), 100, null,
                step("Enfriar", 60, 0, ProcessTimeType.WAITING)));
        assertThat(waiting.estimatedPersonMinutes()).isZero();
        assertThat(waiting.waitingMinutes()).isEqualTo(60);
    }

    @Test
    void rejectsInvalidReferenceStepsFieldsAndIds() {
        assertThatThrownBy(() -> service.createProcess(request(
                carne.getVarietyId(), 0, null,
                step("Preparar", 10, 1, ProcessTimeType.ACTIVE))))
                .isInstanceOf(InvalidProcessReferenceYieldException.class);
        assertThatThrownBy(() -> service.createProcess(request(
                carne.getVarietyId(), 100, null)))
                .isInstanceOf(ProcessWithoutStepsException.class);
        assertThatThrownBy(() -> service.createProcess(null))
                .isInstanceOf(InvalidProductionProcessException.class);
        assertThatThrownBy(() -> service.createProcess(request(
                carne.getVarietyId(), 100, "x".repeat(1001),
                step("Preparar", 10, 1, ProcessTimeType.ACTIVE))))
                .isInstanceOf(InvalidProductionProcessException.class);
        assertThatThrownBy(() -> service.createProcess(request(
                carne.getVarietyId(), 100, null,
                step(" ", 10, 1, ProcessTimeType.ACTIVE))))
                .isInstanceOf(InvalidProductionProcessException.class);
        assertThatThrownBy(() -> service.getProcessById(-1L))
                .isInstanceOf(InvalidProductionProcessException.class);
        assertThatThrownBy(() -> service.getProcessById(Long.MAX_VALUE))
                .isInstanceOf(ProductionProcessNotFoundException.class);
    }

    @Test
    void rejectsSecondCurrentProcessAndMissingVariety() {
        createProcess();
        assertThatThrownBy(this::createProcess)
                .isInstanceOf(ProcessAlreadyExistsException.class)
                .hasMessage("La variedad Carne ya tiene un proceso vigente.");
        assertThatThrownBy(() -> service.createProcess(request(
                Long.MAX_VALUE, 100, null,
                step("Preparar", 10, 1, ProcessTimeType.ACTIVE))))
                .isInstanceOf(VarietyNotFoundException.class);
    }

    @Test
    void listsCurrentHistoricalProcessesAndValidatesSort() {
        ProductionProcessResponseDTO first = createProcess();
        service.createNewVersion(first.id(), versionRequest(
                100, "v2", step("Preparar", 20, 1, ProcessTimeType.ACTIVE)));
        EmpanadaVariety pollo = variety("Pollo");
        service.createProcess(request(pollo.getVarietyId(), 50, null,
                step("Cocinar", 30, 1, ProcessTimeType.ACTIVE)));

        assertThat(service.getActiveProcesses(PageRequest.of(0, 1)).getTotalElements())
                .isEqualTo(2);
        assertThat(service.getProcessesByStatus(false, PAGE).getTotalElements()).isEqualTo(1);
        assertThat(service.getProcessHistory(carne.getVarietyId())).hasSize(2);
        assertThatThrownBy(() -> service.getProcessesByStatus(null, PAGE))
                .isInstanceOf(InvalidProductionProcessException.class);
        assertThatThrownBy(() -> service.getActiveProcesses(PageRequest.of(0, 10)
                .withSort(org.springframework.data.domain.Sort.by("totalEstimatedMinutes"))))
                .isInstanceOf(InvalidProductionProcessException.class);
        assertThat(service.getActiveProcesses(PageRequest.of(0, 10)
                .withSort(org.springframework.data.domain.Sort.by("varietyName"))).getContent())
                .extracting(ProductionProcessResponseDTO::varietyName)
                .containsExactly("Carne", "Pollo");
    }

    @Test
    void concurrentFirstProcessesKeepExactlyOneCurrentProcess() throws Exception {
        List<Boolean> outcomes = concurrently(8, () -> {
            try {
                createProcess();
                return true;
            } catch (ProcessAlreadyExistsException expected) {
                return false;
            }
        });

        assertThat(outcomes.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(processRepository.findByActiveTrue(PAGE).getTotalElements()).isEqualTo(1);
    }

    @Test
    void concurrentVersionsAreSequentialAndKeepOneCurrentProcess() throws Exception {
        ProductionProcessResponseDTO first = createProcess();
        List<Integer> versions = concurrently(5, () -> service.createNewVersion(
                first.id(), versionRequest(100, null,
                        step("Preparar", 20, 1, ProcessTimeType.ACTIVE))).version());

        assertThat(versions).containsExactlyInAnyOrder(2, 3, 4, 5, 6);
        assertThat(processRepository.findByActiveTrue(PAGE).getTotalElements()).isEqualTo(1);
        assertThat(service.getProcessHistory(carne.getVarietyId()))
                .extracting(ProductionProcessResponseDTO::version)
                .containsExactly(6, 5, 4, 3, 2, 1);
    }

    @Test
    void apiRequiresAuthenticationAndExposesFullContract() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        String createBody = body(request(carne.getVarietyId(), 100, "Proceso estándar",
                step("Preparar", 20, 1, ProcessTimeType.ACTIVE),
                step("Enfriar", 60, 0, ProcessTimeType.WAITING)));
        String response = mockMvc.perform(post(BASE).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith(BASE + "/")))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.stepCount").value(2))
                .andExpect(jsonPath("$.totalEstimatedMinutes").value(80))
                .andExpect(jsonPath("$.activeMinutes").value(20))
                .andExpect(jsonPath("$.waitingMinutes").value(60))
                .andExpect(jsonPath("$.estimatedPersonMinutes").value(20))
                .andExpect(jsonPath("$.estimatedPersonHours").value(0.33))
                .andExpect(jsonPath("$.steps[0].stepOrder").value(1))
                .andReturn().getResponse().getContentAsString();
        long processId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        mockMvc.perform(get(BASE).with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(BASE + "/" + processId).with(jwt()))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/variety/" + carne.getVarietyId()).with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(processId));
        mockMvc.perform(get(BASE + "/variety/" + carne.getVarietyId() + "/history")
                        .with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].version").value(1));
        mockMvc.perform(get(BASE + "/status").param("active", "true").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post(BASE + "/" + processId + "/versions").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(versionRequest(100, "v2",
                                step("Preparar", 18, 1, ProcessTimeType.ACTIVE)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void apiReturnsStandardErrorsForInvalidRequests() throws Exception {
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get(BASE + "/9223372036854775807").with(jwt()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/status").with(jwt()))
                .andExpect(status().isBadRequest());

        createProcess();
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(request(carne.getVarietyId(), 100, null,
                                step("Preparar", 10, 1, ProcessTimeType.ACTIVE)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La variedad Carne ya tiene un proceso vigente."));
    }

    private ProductionProcessResponseDTO createProcess() {
        return service.createProcess(request(carne.getVarietyId(), 100, "v1",
                step("Preparar", 20, 1, ProcessTimeType.ACTIVE),
                step("Enfriar", 60, 0, ProcessTimeType.WAITING),
                step("Armar", 50, 2, ProcessTimeType.ACTIVE)));
    }

    private ProductionProcessRequestDTO request(
            Long varietyId,
            Integer referenceYield,
            String notes,
            ProductionProcessStepRequestDTO... steps) {
        return new ProductionProcessRequestDTO(
                varietyId, referenceYield, notes, List.of(steps));
    }

    private ProductionProcessVersionRequestDTO versionRequest(
            Integer referenceYield,
            String notes,
            ProductionProcessStepRequestDTO... steps) {
        return new ProductionProcessVersionRequestDTO(
                referenceYield, notes, List.of(steps));
    }

    private ProductionProcessStepRequestDTO step(
            String name, int minutes, int people, ProcessTimeType type) {
        return new ProductionProcessStepRequestDTO(
                name, "Descripción", minutes, people, type, null);
    }

    private String body(Object value) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
    }

    private EmpanadaVariety variety(String name) {
        return varietyRepository.saveAndFlush(EmpanadaVariety.builder()
                .name(name)
                .unitPrice(new BigDecimal("1500"))
                .halfDozenPrice(new BigDecimal("8000"))
                .dozenPrice(new BigDecimal("15000"))
                .active(1)
                .build());
    }

    private <T> List<T> concurrently(int count, Callable<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No comenzó la prueba concurrente.");
                    }
                    return action.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
