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
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
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
        IngredientResponseDTO result = create("  Carne  ", "15000", "5000", "12.500000");
        assertThat(result.name()).isEqualTo("Carne");
        assertThat(result.active()).isTrue();
        assertThat(result.lowStock()).isFalse();
        assertThat(result.stockValue()).isEqualByComparingTo("187500.00");
        assertThat(result.costPerBaseUnit()).isEqualByComparingTo("12.50");
        assertThat(repository.findById(result.id()).orElseThrow().getCostPerBaseUnit())
                .isEqualByComparingTo("12.50");
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isEqualTo(result.createdAt());
        assertThat(repository.findById(result.id())).isPresent();
    }

    @Test
    void supportsGramMilliliterAndUnitWithTheSameCostFormula() {
        IngredientResponseDTO meat = service.createIngredient(
                request("Carne", MeasurementUnit.GRAM, "5000", "500", "10"));
        IngredientResponseDTO oil = service.createIngredient(
                request("Aceite", MeasurementUnit.MILLILITER, "2000", "250", "4"));
        IngredientResponseDTO egg = service.createIngredient(
                request("Huevo", MeasurementUnit.UNIT, "30", "6", "200"));

        assertThat(meat.stockValue()).isEqualByComparingTo("50000");
        assertThat(oil.stockValue()).isEqualByComparingTo("8000");
        assertThat(egg.stockValue()).isEqualByComparingTo("6000");
        assertThat(List.of(meat.measurementUnit(), oil.measurementUnit(), egg.measurementUnit()))
                .containsExactly(MeasurementUnit.GRAM, MeasurementUnit.MILLILITER,
                        MeasurementUnit.UNIT);
    }

    @Test
    void calculatesBaseCostFromRealPurchasePresentations() {
        IngredientResponseDTO oil = service.createIngredient(new IngredientRequestDTO(
                "Aceite", MeasurementUnit.MILLILITER, "Botella",
                new BigDecimal("900"), new BigDecimal("2740"),
                new BigDecimal("900"), new BigDecimal("300")));
        IngredientResponseDTO beer = service.createIngredient(new IngredientRequestDTO(
                "Cerveza", MeasurementUnit.MILLILITER, "Lata",
                new BigDecimal("473"), new BigDecimal("1800"),
                new BigDecimal("1419"), new BigDecimal("473")));
        IngredientResponseDTO egg = service.createIngredient(new IngredientRequestDTO(
                "Huevo", MeasurementUnit.UNIT, "Maple",
                new BigDecimal("30"), new BigDecimal("6000"),
                new BigDecimal("30"), new BigDecimal("6")));
        IngredientResponseDTO meat = service.createIngredient(new IngredientRequestDTO(
                "Carne", MeasurementUnit.GRAM, "Bolsa",
                new BigDecimal("1000"), new BigDecimal("9999"),
                new BigDecimal("1000"), new BigDecimal("200")));

        assertThat(oil.costPerBaseUnit()).isEqualByComparingTo("3.044444");
        assertThat(oil.stockValue()).isEqualByComparingTo("2739.999600");
        assertThat(beer.costPerBaseUnit()).isEqualByComparingTo("3.805497");
        assertThat(new BigDecimal("1419").multiply(beer.costPerBaseUnit()))
                .isEqualByComparingTo("5400.000243");
        assertThat(egg.costPerBaseUnit()).isEqualByComparingTo("200");
        assertThat(meat.costPerBaseUnit()).isEqualByComparingTo("9.999");
        assertThat(List.of(oil.purchaseDataComplete(), beer.purchaseDataComplete(),
                egg.purchaseDataComplete(), meat.purchaseDataComplete()))
                .containsOnly(true);
    }

    @Test
    void changingPurchasePresentationRecalculatesCostWithoutChangingStock() {
        IngredientResponseDTO oil = service.createIngredient(new IngredientRequestDTO(
                "Aceite", MeasurementUnit.MILLILITER, "Botella",
                new BigDecimal("900"), new BigDecimal("2740"),
                new BigDecimal("3600"), new BigDecimal("900")));

        IngredientResponseDTO updated = service.updateIngredient(oil.id(),
                new IngredientRequestDTO("Aceite", MeasurementUnit.MILLILITER, "Bidón",
                        new BigDecimal("5000"), new BigDecimal("14000"),
                        new BigDecimal("3600"), new BigDecimal("900")));

        assertThat(updated.purchasePresentation()).isEqualTo("Bidón");
        assertThat(updated.purchaseQuantity()).isEqualByComparingTo("5000");
        assertThat(updated.purchasePrice()).isEqualByComparingTo("14000");
        assertThat(updated.costPerBaseUnit()).isEqualByComparingTo("2.8");
        assertThat(updated.currentStock()).isEqualByComparingTo("3600");
    }

    @Test
    void legacyIngredientKeepsItsExistingCostUntilRealPurchaseDataIsProvided() {
        Ingredient legacy = repository.saveAndFlush(Ingredient.builder()
                .name("Ingrediente histórico")
                .measurementUnit(MeasurementUnit.GRAM)
                .currentStock(new BigDecimal("25"))
                .minimumStock(BigDecimal.ZERO)
                .costPerBaseUnit(new BigDecimal("7.25"))
                .active(true)
                .build());

        IngredientResponseDTO response = service.getIngredientById(legacy.getId());

        assertThat(response.purchasePresentation()).isNull();
        assertThat(response.purchaseQuantity()).isNull();
        assertThat(response.purchasePrice()).isNull();
        assertThat(response.purchaseDataComplete()).isFalse();
        assertThat(response.costPerBaseUnit()).isEqualByComparingTo("7.25");
    }

    @Test
    void createRequiresCompletePurchaseDataAndNeverAcceptsZeroQuantity() throws Exception {
        assertThatThrownBy(() -> service.createIngredient(new IngredientRequestDTO(
                "Aceite", MeasurementUnit.MILLILITER, null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO)))
                .isInstanceOf(InvalidIngredientException.class)
                .hasMessageContaining("presentación de compra es obligatoria");

        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Aceite","measurementUnit":"MILLILITER",
                                "purchasePresentation":"Botella","purchaseQuantity":0,
                                "purchasePrice":2740,"currentStock":900,"minimumStock":300}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "La cantidad de la presentación de compra debe ser mayor a cero.")));
    }

    @Test
    void apiIgnoresClientCalculatedCostAndReturnsBackendCalculation() throws Exception {
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Aceite","measurementUnit":"MILLILITER",
                                "purchasePresentation":"Botella","purchaseQuantity":900,
                                "purchasePrice":2740,"costPerBaseUnit":999,
                                "currentStock":900,"minimumStock":300}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costPerBaseUnit").value(3.044444))
                .andExpect(jsonPath("$.purchaseDataComplete").value(true));
    }

    @Test
    void measurementUnitBecomesImmutableOnceIngredientHasStock() {
        IngredientResponseDTO ingredient = service.createIngredient(
                request("Aceite", MeasurementUnit.MILLILITER, "1", "0", "4"));

        assertThatThrownBy(() -> service.updateIngredient(ingredient.id(),
                request("Aceite", MeasurementUnit.UNIT, "1", "0", "4")))
                .isInstanceOf(InvalidIngredientException.class)
                .hasMessageContaining("unidad de medida no puede cambiarse");
        assertThat(service.getIngredientById(ingredient.id()).measurementUnit())
                .isEqualTo(MeasurementUnit.MILLILITER);
    }

    @Test
    void rejectsDuplicateNamesEvenWhenOriginalIsInactive() {
        IngredientResponseDTO original = create("Carne", "10", "1", "2");
        service.deactivateIngredient(original.id());
        assertThatThrownBy(() -> create(" cARNe ", "1", "0", "1"))
                .isInstanceOf(IngredientAlreadyExistsException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void updatePreservesIdentityStatusAndCreationDate() {
        IngredientResponseDTO original = create("Carne", "10", "1", "2");
        service.deactivateIngredient(original.id());
        IngredientResponseDTO updated = service.updateIngredient(original.id(), request("CARNE", "20", "5", "3"));
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.active()).isFalse();
        assertThat(updated.updatedAt()).isAfterOrEqualTo(original.updatedAt());
        assertThat(updated.stockValue()).isEqualByComparingTo("60");
        assertThat(service.getIngredientById(original.id()).name()).isEqualTo("CARNE");
    }

    @Test
    void rejectsRenameToAnotherIngredientWithoutChangingStoredData() {
        create("Carne", "10", "1", "2");
        IngredientResponseDTO cheese = create("Queso", "10", "1", "2");
        assertThatThrownBy(() -> service.updateIngredient(cheese.id(), request("CARNE", "20", "5", "3")))
                .isInstanceOf(IngredientAlreadyExistsException.class);
        assertThat(service.getIngredientById(cheese.id()).name()).isEqualTo("Queso");
    }

    @Test
    void stockMutationsAreExactAndRejectInsufficientStock() {
        Long id = create("Queso", "1.25", "0.50", "123.456").id();
        assertThat(service.increaseStock(id, movement("0.10")).currentStock()).isEqualByComparingTo("1.35");
        assertThat(service.decreaseStock(id, movement("0.85")).lowStock()).isTrue();
        assertThatThrownBy(() -> service.decreaseStock(id, movement("0.51")))
                .isInstanceOf(InsufficientIngredientStockException.class)
                .hasMessageContaining("Queso").hasMessageContaining("0.51");
        assertThat(service.getIngredientById(id).currentStock()).isEqualByComparingTo("0.50");
        assertThat(service.decreaseStock(id, movement("0.50")).currentStock()).isZero();
        assertThat(service.setStock(id, new IngredientStockUpdateDTO(new BigDecimal("12.75")))
                .currentStock()).isEqualByComparingTo("12.75");
    }

    @Test
    void costAndMinimumUpdatesChangeOnlyTheirOwnFields() {
        IngredientResponseDTO original = create("Carne", "10", "1", "2000");
        IngredientResponseDTO cost = service.updateCost(original.id(), costUpdate("Bolsa", "1000000", "1000"));
        assertThat(cost.costPerBaseUnit()).isEqualByComparingTo("0.001");
        assertThat(cost.stockValue()).isEqualByComparingTo("0.010");
        assertThat(cost.currentStock()).isEqualByComparingTo("10");
        assertThat(cost.minimumStock()).isEqualByComparingTo("1");
        IngredientResponseDTO minimum = service.updateMinimumStock(original.id(), new IngredientMinimumStockDTO(BigDecimal.TEN));
        assertThat(minimum.lowStock()).isTrue();
        assertThat(minimum.costPerBaseUnit()).isEqualByComparingTo(cost.costPerBaseUnit());
        assertThat(minimum.name()).isEqualTo(original.name());
    }

    @Test
    void inactiveIngredientsRemainAvailableForAdministrationButCannotBeConsumed() {
        Long id = create("Carne", "10", "1", "2").id();
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
        assertThat(service.decreaseStock(id, movement("1")).currentStock()).isEqualByComparingTo("4");
    }

    @Test
    void paginatesFiltersAndSearchesLiteralTextIgnoringCase() {
        create("Carne", "10", "1", "2");
        Long inactive = create("Carne suave", "0", "1", "2").id();
        service.deactivateIngredient(inactive);
        create("Queso", "1", "1", "3");
        create("Cacao 100%", "1", "1", "4");
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
        create("Carne", "10", "1", "2");
        create("Queso", "1", "1", "3");
        Long inactive = create("Harina", "2", "5", "4").id();
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
    @ValueSource(strings = {"-1", "0.00001", "1000000000000000"})
    void rejectsInvalidStockAtServiceBoundary(String stock) {
        assertThatThrownBy(() -> create("Carne", stock, "0", "1"))
                .isInstanceOf(InvalidIngredientException.class);
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.0000001", "10000000000000"})
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
        assertThatThrownBy(() -> service.createIngredient(
                new IngredientRequestDTO("Carne", null, null, null, null, null, null)))
                .isInstanceOf(InvalidIngredientException.class);
    }

    @Test
    void invalidMutationsAndOverflowRollBack() {
        Long id = create("Carne", "999999999999999.9999", "0", "1").id();
        assertThatThrownBy(() -> service.increaseStock(id, movement("0.0001")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.increaseStock(id, movement("0")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.decreaseStock(id, movement("-1")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.setStock(id, new IngredientStockUpdateDTO(new BigDecimal("-1"))))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.updateMinimumStock(id, new IngredientMinimumStockDTO(new BigDecimal("-1"))))
                .isInstanceOf(InvalidIngredientException.class);
        assertThatThrownBy(() -> service.updateCost(id,
                costUpdate("Bolsa", "1", "-1")))
                .isInstanceOf(InvalidIngredientException.class);
        assertThat(service.getIngredientById(id).currentStock()).isEqualByComparingTo("999999999999999.9999");
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
        Long id = create("Carne", "5", "0", "1").id();
        List<Boolean> results = concurrently(12, () -> {
            try {
                service.decreaseStock(id, movement("1"));
                return true;
            } catch (InsufficientIngredientStockException expected) {
                return false;
            }
        });
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(5);
        assertThat(service.getIngredientById(id).currentStock()).isZero();
    }

    @Test
    void concurrentIncreasesDoNotLoseUpdates() throws Exception {
        Long id = create("Carne", "0", "0", "1").id();
        concurrently(12, () -> {
            service.increaseStock(id, movement("0.01"));
            return true;
        });
        assertThat(service.getIngredientById(id).currentStock()).isEqualByComparingTo("0.12");
    }

    @Test
    void concurrentCreatesKeepOneIngredientAndReturnDomainConflicts() throws Exception {
        List<Boolean> results = concurrently(8, () -> {
            try {
                create("Carne", "1", "0", "1");
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
    @ValueSource(strings = {"-1", "0.00001", "1000000000000000"})
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
                        .content(body("Carne", "15000", "5000", "12.5")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")))
                .andExpect(jsonPath("$.name").value("Carne"))
                .andExpect(jsonPath("$.stockValue").value(187500))
                .andExpect(jsonPath("$.costPerBaseUnit").value(12.5))
                .andExpect(jsonPath("$.measurementUnit").value("GRAM"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void apiExposesAllReadEndpoints() throws Exception {
        Long id = create("Carne", "1", "1", "2").id();
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
        Long id = create("Carne", "10", "1", "2").id();
        String path = BASE + "/" + id;
        mockMvc.perform(put(path).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Carne picada", "20", "5", "3")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Carne picada"));
        patchValue(path + "/stock", "currentStock", "30", "currentStock", 30);
        patchValue(path + "/stock/increase", "quantity", "5", "currentStock", 35);
        patchValue(path + "/stock/decrease", "quantity", "10", "currentStock", 25);
        mockMvc.perform(patch(path + "/cost").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchasePresentation":"Bolsa","purchaseQuantity":1000,"purchasePrice":4000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costPerBaseUnit").value(4));
        patchValue(path + "/minimum-stock", "minimumStock", "25", "minimumStock", 25);
        mockMvc.perform(patch(path + "/deactivate").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(patch(path + "/activate").with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void apiUsesStandardErrorsForInvalidMissingAndConflictingIngredients() throws Exception {
        Long id = create("Carne", "1", "0", "1").id();
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get(BASE + "/9223372036854775807").with(jwt()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("CARNE", "1", "0", "1")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Ya existe un ingrediente llamado 'CARNE'."));
        mockMvc.perform(patch(BASE + "/" + id + "/stock/decrease").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(
                        "Stock insuficiente de Carne. Disponible: 1.0000 g. Solicitado: 2 g."));
        service.deactivateIngredient(id);
        mockMvc.perform(patch(BASE + "/" + id + "/stock/decrease").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.path").value(BASE + "/" + id + "/stock/decrease"));
    }

    @Test
    void baseUnitCostRoundTripsWithoutRoundingAndUpdatesStockValue() {
        Long id = create("Carne", "250", "0", "12.500125").id();
        IngredientResponseDTO stored = service.getIngredientById(id);
        assertThat(stored.costPerBaseUnit()).isEqualByComparingTo("12.500125");
        assertThat(stored.stockValue()).isEqualByComparingTo("3125.03125");

        service.updateCost(id, costUpdate("Bolsa", "1000000", "14350250"));
        stored = service.getIngredientById(id);
        assertThat(stored.costPerBaseUnit()).isEqualByComparingTo("14.35025");
        assertThat(stored.stockValue()).isEqualByComparingTo("3587.5625");
        assertThat(service.getSummary().totalStockValue()).isEqualByComparingTo("3587.5625");
    }

    @Test
    void baseUnitCostSortingWorksForAllPaginatedEndpoints() throws Exception {
        create("Carne económica", "1", "0", "8");
        create("Carne premium", "1", "0", "12.5");
        for (String path : List.of(BASE, BASE + "/search?query=Carne", BASE + "/status?active=true")) {
            mockMvc.perform(get(path).param("sort", "costPerBaseUnit,desc").with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Carne premium"))
                    .andExpect(jsonPath("$.content[0].costPerBaseUnit").value(12.5));
        }
    }

    @Test
    void apiRequiresMeasurementUnitAndRejectsExcessCostPrecision() throws Exception {
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Carne","currentStock":1000,"minimumStock":0,"costPerBaseUnit":12.5}
                                """))
                .andExpect(status().isBadRequest());
        Long id = create("Carne", "1000", "0", "12.5").id();
        mockMvc.perform(patch(BASE + "/" + id + "/cost").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchasePresentation":"Bolsa","purchaseQuantity":0,"purchasePrice":100}
                                """))
                .andExpect(status().isBadRequest());
        assertThat(service.getIngredientById(id).costPerBaseUnit()).isEqualByComparingTo("12.5");
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
        return request(name, MeasurementUnit.GRAM, stock, minimum, cost);
    }

    private IngredientRequestDTO request(String name, MeasurementUnit unit,
            String stock, String minimum, String cost) {
        BigDecimal purchaseQuantity = new BigDecimal("1000000");
        BigDecimal purchasePrice = new BigDecimal(cost).multiply(purchaseQuantity)
                .stripTrailingZeros();
        return new IngredientRequestDTO(name, unit, "Presentación de prueba",
                purchaseQuantity, purchasePrice, new BigDecimal(stock),
                new BigDecimal(minimum));
    }

    private IngredientCostUpdateDTO costUpdate(String presentation, String quantity,
            String price) {
        return new IngredientCostUpdateDTO(presentation, new BigDecimal(quantity),
                new BigDecimal(price));
    }

    private IngredientStockMovementDTO movement(String value) {
        return new IngredientStockMovementDTO(new BigDecimal(value));
    }

    private String body(String name, String stock, String minimum, String cost) {
        BigDecimal purchaseQuantity = new BigDecimal("1000000");
        BigDecimal purchasePrice = new BigDecimal(cost).multiply(purchaseQuantity)
                .stripTrailingZeros();
        return """
                {"name":"%s","measurementUnit":"GRAM","purchasePresentation":"Presentación de prueba",\
                "purchaseQuantity":%s,"purchasePrice":%s,"currentStock":%s,"minimumStock":%s}
                """.formatted(name, purchaseQuantity.toPlainString(),
                        purchasePrice.toPlainString(), stock, minimum);
    }
}
