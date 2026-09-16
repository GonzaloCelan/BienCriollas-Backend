package com.bienCriollas.stock.production.ingredient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.bienCriollas.stock.production.ingredient.dto.*;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.exception.*;
import com.bienCriollas.stock.production.ingredient.interfaces.IIngredientService;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ingredients-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.hikari.maximum-pool-size=12"
})
@AutoConfigureMockMvc
class IngredientIntegrationTest {

    private static final String BASE = "/api/v1/ingredients";
    private static final PageRequest PAGE = PageRequest.of(0, 10);

    @Autowired private IIngredientService service;
    @Autowired private IngredientRepository repository;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void clearIngredients() {
        repository.deleteAll();
    }

    @Test
    void createsNormalizedIngredientWithCalculatedFieldsAndDates() {
        IngredientResponseDTO result = create("  Carne  ", "15000", "5000", "12500.00");
        assertThat(result.name()).isEqualTo("Carne");
        assertThat(result.active()).isTrue();
        assertThat(result.lowStock()).isFalse();
        assertThat(result.stockValue()).isEqualByComparingTo("187500.00");
        assertThat(result.costPerKilogram()).isEqualByComparingTo("12500");
        assertThat(result.costPerGram()).isEqualByComparingTo("12.50");
        assertThat(repository.findById(result.id()).orElseThrow().getCostPerKilogram())
                .isEqualByComparingTo("12500");
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isEqualTo(result.createdAt());
        assertThat(repository.findById(result.id())).isPresent();
    }

    @Test
    void rejectsDuplicateNamesEvenWhenOriginalIsInactive() {
        IngredientResponseDTO original = create("Carne", "10", "1", "2000");
        service.deactivateIngredient(original.id());
        assertThatThrownBy(() -> create(" cARNe ", "1", "0", "1000"))
                .isInstanceOf(IngredientAlreadyExistsException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void updatePreservesIdentityStatusAndCreationDate() {
        IngredientResponseDTO original = create("Carne", "10", "1", "2000");
        service.deactivateIngredient(original.id());
        IngredientResponseDTO updated = service.updateIngredient(original.id(), request("CARNE", "20", "5", "3000"));
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.active()).isFalse();
        assertThat(updated.updatedAt()).isAfterOrEqualTo(original.updatedAt());
        assertThat(updated.stockValue()).isEqualByComparingTo("60");
        assertThat(service.getIngredientById(original.id()).name()).isEqualTo("CARNE");
    }

    @Test
    void rejectsRenameToAnotherIngredientWithoutChangingStoredData() {
        create("Carne", "10", "1", "2000");
        IngredientResponseDTO cheese = create("Queso", "10", "1", "2000");
        assertThatThrownBy(() -> service.updateIngredient(cheese.id(), request("CARNE", "20", "5", "3000")))
                .isInstanceOf(IngredientAlreadyExistsException.class);
        assertThat(service.getIngredientById(cheese.id()).name()).isEqualTo("Queso");
    }

    @Test
    void stockMutationsAreExactAndRejectInsufficientStock() {
        Long id = create("Queso", "1.25", "0.50", "123.456").id();
        assertThat(service.increaseStock(id, movement("0.10")).currentStockGrams()).isEqualByComparingTo("1.35");
        assertThat(service.decreaseStock(id, movement("0.85")).lowStock()).isTrue();
        assertThatThrownBy(() -> service.decreaseStock(id, movement("0.51")))
                .isInstanceOf(InsufficientIngredientStockException.class)
                .hasMessageContaining("Queso").hasMessageContaining("0.51");
        assertThat(service.getIngredientById(id).currentStockGrams()).isEqualByComparingTo("0.50");
        assertThat(service.decreaseStock(id, movement("0.50")).currentStockGrams()).isZero();
        assertThat(service.setStock(id, new IngredientStockUpdateDTO(new BigDecimal("12.75")))
                .currentStockGrams()).isEqualByComparingTo("12.75");
    }

    @Test
    void costAndMinimumUpdatesChangeOnlyTheirOwnFields() {
        IngredientResponseDTO original = create("Carne", "10", "1", "2000");
        IngredientResponseDTO cost = service.updateCost(original.id(), new IngredientCostUpdateDTO(new BigDecimal("0.001")));
        assertThat(cost.costPerGram()).isEqualByComparingTo("0.000001");
        assertThat(cost.costPerKilogram()).isEqualByComparingTo("0.001");
        assertThat(cost.stockValue()).isEqualByComparingTo("0.000010");
        assertThat(cost.currentStockGrams()).isEqualByComparingTo("10");
        assertThat(cost.minimumStockGrams()).isEqualByComparingTo("1");
        IngredientResponseDTO minimum = service.updateMinimumStock(original.id(), new IngredientMinimumStockDTO(BigDecimal.TEN));
        assertThat(minimum.lowStock()).isTrue();
        assertThat(minimum.costPerGram()).isEqualByComparingTo(cost.costPerGram());
        assertThat(minimum.name()).isEqualTo(original.name());
    }

    @Test
    void inactiveIngredientsRemainAvailableForAdministrationButCannotBeConsumed() {
        Long id = create("Carne", "10", "1", "2000").id();
        service.deactivateIngredient(id);
        assertThat(service.getIngredients(PAGE)).isEmpty();
        assertThat(service.getIngredientsByStatus(false, PAGE).getTotalElements()).isEqualTo(1);
        assertThat(service.getIngredientById(id).active()).isFalse();
        assertThatThrownBy(() -> service.decreaseStock(id, movement("1")))
                .isInstanceOf(IngredientInactiveException.class);
        Ingredient ingredient = repository.findById(id).orElseThrow();
        assertThatThrownBy(ingredient::requireActive).isInstanceOf(IngredientInactiveException.class);
        service.setStock(id, new IngredientStockUpdateDTO(new BigDecimal("5")));
        service.activateIngredient(id);
        assertThat(service.decreaseStock(id, movement("1")).currentStockGrams()).isEqualByComparingTo("4");
    }

    @Test
    void paginatesFiltersAndSearchesLiteralTextIgnoringCase() {
        create("Carne", "10", "1", "2000");
        Long inactive = create("Carne suave", "0", "1", "2000").id();
        service.deactivateIngredient(inactive);
        create("Queso", "1", "1", "3000");
        create("Cacao 100%", "1", "1", "4000");
        assertThat(service.getIngredients(PageRequest.of(0, 2)).getTotalElements()).isEqualTo(3);
        assertThat(service.getIngredients(PageRequest.of(0, 2)).getContent()).hasSize(2);
        assertThat(service.searchIngredients("CAR", PAGE).getContent())
                .extracting(IngredientResponseDTO::name).containsExactly("Carne");
        assertThat(service.searchIngredients("%", PAGE).getContent())
                .extracting(IngredientResponseDTO::name).containsExactly("Cacao 100%");
        assertThat(service.searchIngredients("_", PAGE)).isEmpty();
        assertThat(service.getLowStockIngredients()).extracting(IngredientResponseDTO::name)
                .containsExactly("Cacao 100%", "Queso");
    }

    @Test
    void summaryIncludesAllStockValueAndOnlyActiveLowStock() {
        create("Carne", "10", "1", "2000");
        create("Queso", "1", "1", "3000");
        Long inactive = create("Harina", "2", "5", "4000").id();
        service.deactivateIngredient(inactive);
        IngredientSummaryDTO summary = service.getSummary();
        assertThat(summary.totalIngredients()).isEqualTo(3);
        assertThat(summary.activeIngredients()).isEqualTo(2);
        assertThat(summary.inactiveIngredients()).isEqualTo(1);
        assertThat(summary.lowStockIngredients()).isEqualTo(1);
        assertThat(summary.totalStockValue()).isEqualByComparingTo("31");
    }

    @Test
    void emptySummaryReturnsZeroes() {
        IngredientSummaryDTO summary = service.getSummary();
        assertThat(summary.totalIngredients()).isZero();
        assertThat(summary.activeIngredients()).isZero();
        assertThat(summary.inactiveIngredients()).isZero();
        assertThat(summary.lowStockIngredients()).isZero();
        assertThat(summary.totalStockValue()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.001", "1000000000000"})
    void rejectsInvalidStockAtServiceBoundary(String stock) {
        assertThatThrownBy(() -> create("Carne", stock, "0", "1"))
                .isInstanceOf(InvalidIngredientException.class);
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.0001", "100000000000"})
    void rejectsInvalidCostsAtServiceBoundary(String cost) {
        assertThatThrownBy(() -> create("Carne", "1", "0", cost))
                .isInstanceOf(InvalidIngredientException.class);
    }

    @Test
    void rejectsInvalidNamesNullFieldsAndInvalidMinimumStock() {
        assertThatThrownBy(() -> create(" ", "1", "0", "1000")).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> create("x".repeat(101), "1", "0", "1")).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> create("Carne", "1", "-1", "1000")).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.createIngredient(null)).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.createIngredient(new IngredientRequestDTO("Carne", null, null, null)))
                .isInstanceOf(InvalidIngredientException.class);
    }

    @Test
    void invalidMutationsAndOverflowRollBack() {
        Long id = create("Carne", "999999999999.99", "0", "1000").id();
        assertThatThrownBy(() -> service.increaseStock(id, movement("0.01")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.increaseStock(id, movement("0")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.decreaseStock(id, movement("-1")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.setStock(id, new IngredientStockUpdateDTO(new BigDecimal("-1"))))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.updateMinimumStock(id, new IngredientMinimumStockDTO(new BigDecimal("-1"))))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.updateCost(id, new IngredientCostUpdateDTO(BigDecimal.ZERO)))
                .isInstanceOf(InvalidIngredientException.class);
        assertThat(service.getIngredientById(id).currentStockGrams()).isEqualByComparingTo("999999999999.99");
    }

    @Test
    void rejectsInvalidQueryStatusAndUnknownIds() {
        assertThatThrownBy(() -> service.searchIngredients(" ", PAGE)).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.getIngredientsByStatus(null, PAGE)).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.getIngredientById(-1L)).isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.getIngredientById(Long.MAX_VALUE)).isInstanceOf(IngredientNotFoundException.class);
        assertThatThrownBy(() -> service.activateIngredient(Long.MAX_VALUE)).isInstanceOf(IngredientNotFoundException.class);
    }

    @Test
    void concurrentDecreasesNeverOversell() throws Exception {
        Long id = create("Carne", "5", "0", "1000").id();
        List<Boolean> results = concurrently(12, () -> {
            try {
                service.decreaseStock(id, movement("1"));
                return true;
            } catch (InsufficientIngredientStockException expected) {
                return false;
            }
        });
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(5);
        assertThat(service.getIngredientById(id).currentStockGrams()).isZero();
    }

    @Test
    void concurrentIncreasesDoNotLoseUpdates() throws Exception {
        Long id = create("Carne", "0", "0", "1000").id();
        concurrently(12, () -> {
            service.increaseStock(id, movement("0.01"));
            return true;
        });
        assertThat(service.getIngredientById(id).currentStockGrams()).isEqualByComparingTo("0.12");
    }

    @Test
    void concurrentCreatesKeepOneIngredientAndReturnDomainConflicts() throws Exception {
        List<Boolean> results = concurrently(8, () -> {
            try {
                create("Carne", "1", "0", "1000");
                return true;
            } catch (IngredientAlreadyExistsException expected) {
                return false;
            }
        });
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void apiRejectsMissingParametersAndUnknownSortFields() throws Exception {
        mockMvc.perform(get(BASE + "/search").with(jwt())).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/status").with(jwt())).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE).param("sort", "stockValue,desc").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.001", "1000000000000"})
    void apiRejectsInvalidStockPrecisionAndRange(String stock) throws Exception {
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Carne", stock, "0", "1")))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void apiRequiresAuthenticationAndReturnsCreatedLocation() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Carne", "15000", "5000", "12500.00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")))
                .andExpect(jsonPath("$.name").value("Carne"))
                .andExpect(jsonPath("$.stockValue").value(187500))
                .andExpect(jsonPath("$.costPerKilogram").value(12500))
                .andExpect(jsonPath("$.costPerGram").value(12.5))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void apiExposesAllReadEndpoints() throws Exception {
        Long id = create("Carne", "1", "1", "2000").id();
        mockMvc.perform(get(BASE).with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));
        mockMvc.perform(get(BASE + "/" + id).with(jwt())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/search").param("query", "CAR").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(BASE + "/status").param("active", "false").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get(BASE + "/low-stock").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lowStock").value(true));
        mockMvc.perform(get(BASE + "/summary").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngredients").value(1));
    }

    @Test
    void apiExposesAllUpdateEndpoints() throws Exception {
        Long id = create("Carne", "10", "1", "2000").id();
        String path = BASE + "/" + id;
        mockMvc.perform(put(path).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Carne picada", "20", "5", "3000")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Carne picada"));
        patchValue(path + "/stock", "stockGrams", "30", "currentStockGrams", 30);
        patchValue(path + "/stock/increase", "quantityGrams", "5", "currentStockGrams", 35);
        patchValue(path + "/stock/decrease", "quantityGrams", "10", "currentStockGrams", 25);
        patchValue(path + "/cost", "costPerKilogram", "4000", "costPerKilogram", 4000);
        patchValue(path + "/minimum-stock", "minimumStockGrams", "25", "minimumStockGrams", 25);
        mockMvc.perform(patch(path + "/deactivate").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch(path + "/activate").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void apiUsesStandardErrorsForInvalidMissingAndConflictingIngredients() throws Exception {
        Long id = create("Carne", "1", "0", "1000").id();
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get(BASE + "/9223372036854775807").with(jwt()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("CARNE", "1", "0", "1000")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Ya existe un ingrediente llamado 'CARNE'."));
        mockMvc.perform(patch(BASE + "/" + id + "/stock/decrease").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityGrams\":2}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(
                        "Stock insuficiente de Carne. Disponible: 1.00 g. Solicitado: 2 g."));
        service.deactivateIngredient(id);
        mockMvc.perform(patch(BASE + "/" + id + "/stock/decrease").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityGrams\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.path").value(BASE + "/" + id + "/stock/decrease"));
    }

    @Test
    void kilogramPriceRoundTripsWithoutRoundingAndUpdatesStockValue() {
        Long id = create("Carne", "250", "0", "12500.125").id();
        IngredientResponseDTO stored = service.getIngredientById(id);
        assertThat(stored.costPerKilogram()).isEqualByComparingTo("12500.125");
        assertThat(stored.costPerGram()).isEqualByComparingTo("12.500125");
        assertThat(stored.stockValue()).isEqualByComparingTo("3125.03125");

        service.updateCost(id, new IngredientCostUpdateDTO(new BigDecimal("14350.25")));
        stored = service.getIngredientById(id);
        assertThat(stored.costPerKilogram()).isEqualByComparingTo("14350.25");
        assertThat(stored.costPerGram()).isEqualByComparingTo("14.35025");
        assertThat(stored.stockValue()).isEqualByComparingTo("3587.5625");
        assertThat(service.getSummary().totalStockValue()).isEqualByComparingTo("3587.5625");
    }

    @Test
    void kilogramPriceSortingWorksForAllPaginatedEndpoints() throws Exception {
        create("Carne económica", "1", "0", "8000");
        create("Carne premium", "1", "0", "12500");
        for (String path : List.of(BASE, BASE + "/search?query=Carne", BASE + "/status?active=true")) {
            mockMvc.perform(get(path).param("sort", "costPerKilogram,desc").with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Carne premium"))
                    .andExpect(jsonPath("$.content[0].costPerKilogram").value(12500));
        }
    }

    @Test
    void apiRequiresKilogramPriceAndRejectsExcessPrecision() throws Exception {
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Carne","currentStockGrams":1000,"minimumStockGrams":0,"costPerGram":12.5}
                                """))
                .andExpect(status().isBadRequest());
        Long id = create("Carne", "1000", "0", "12500").id();
        mockMvc.perform(patch(BASE + "/" + id + "/cost").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"costPerKilogram\":12500.1234}"))
                .andExpect(status().isBadRequest());
        assertThat(service.getIngredientById(id).costPerKilogram()).isEqualByComparingTo("12500");
    }

    private void patchValue(String path, String field, String value, String responseField, int expected) throws Exception {
        mockMvc.perform(patch(path).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"" + field + "\":" + value + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$." + responseField).value(expected));
    }

    private <T> List<T> concurrently(int count, Callable<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se inició la prueba concurrente.");
                    }
                    return task.call();
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

    private IngredientResponseDTO create(String name, String stock, String minimum, String cost) {
        return service.createIngredient(request(name, stock, minimum, cost));
    }

    private IngredientRequestDTO request(String name, String stock, String minimum, String cost) {
        return new IngredientRequestDTO(name, new BigDecimal(stock), new BigDecimal(minimum), new BigDecimal(cost));
    }

    private IngredientStockMovementDTO movement(String value) {
        return new IngredientStockMovementDTO(new BigDecimal(value));
    }

    private String body(String name, String stock, String minimum, String cost) {
        return """
                {"name":"%s","currentStockGrams":%s,"minimumStockGrams":%s,"costPerKilogram":%s}
                """.formatted(name, stock, minimum, cost);
    }
}
