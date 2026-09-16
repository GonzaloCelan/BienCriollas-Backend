package com.bienCriollas.stock.production;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bienCriollas.stock.production.dto.*;
import com.bienCriollas.stock.production.analytics.entity.ProductionCostSettings;
import com.bienCriollas.stock.production.analytics.repository.ProductionCostSettingsRepository;
import com.bienCriollas.stock.production.entity.Production;
import com.bienCriollas.stock.production.enums.ProductionStatus;
import com.bienCriollas.stock.production.exception.*;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.exception.InsufficientIngredientStockException;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;
import com.bienCriollas.stock.production.interfaces.IProductionService;
import com.bienCriollas.stock.production.process.entity.*;
import com.bienCriollas.stock.production.process.enums.ProcessTimeType;
import com.bienCriollas.stock.production.process.repository.ProductionProcessRepository;
import com.bienCriollas.stock.production.recipe.entity.*;
import com.bienCriollas.stock.production.recipe.repository.RecipeRepository;
import com.bienCriollas.stock.production.repository.*;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.InactiveVarietyException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:production-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ProductionIntegrationTest {

    private static final String BASE = "/api/v1/productions";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

    @Autowired private IProductionService service;
    @Autowired private ProductionRepository productionRepository;
    @Autowired private ProductionIngredientRepository productionIngredientRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private ProductionProcessRepository processRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private EmpanadaVarietyRepository varietyRepository;
    @Autowired private ProductionCostSettingsRepository costSettingsRepository;
    @Autowired private MockMvc mockMvc;

    private EmpanadaVariety carne;
    private Ingredient meat;
    private Ingredient onion;

    @BeforeEach
    void setUp() {
        productionIngredientRepository.deleteAll();
        productionRepository.deleteAll();
        costSettingsRepository.deleteAll();
        processRepository.deleteAll();
        recipeRepository.deleteAll();
        stockRepository.deleteAll();
        ingredientRepository.deleteAll();
        varietyRepository.deleteAll();

        costSettingsRepository.saveAndFlush(ProductionCostSettings.builder()
                .averageHourlyLaborCost(new BigDecimal("5000.00"))
                .energyPercentage(new BigDecimal("6.00")).build());

        carne = varietyRepository.saveAndFlush(EmpanadaVariety.builder()
                .name("Carne").unitPrice(new BigDecimal("1500"))
                .halfDozenPrice(new BigDecimal("8000"))
                .dozenPrice(new BigDecimal("15000")).active(1).build());
        meat = ingredient("Carne picada", "25000.00", "12500.000");
        onion = ingredient("Cebolla", "15000.00", "1500.000");
        createRecipe();
        createProcess();
    }

    @Test
    void createsDraftFromCurrentRecipeAndProcessWithScaledSnapshot() {
        ProductionResponseDTO result = service.createProduction(
                new ProductionCreateRequestDTO(carne.getVarietyId(), DATE, 250, " Tanda tarde "));

        assertThat(result.status()).isEqualTo(ProductionStatus.DRAFT);
        assertThat(result.recipeVersion()).isEqualTo(1);
        assertThat(result.processVersion()).isEqualTo(1);
        assertThat(result.notes()).isEqualTo("Tanda tarde");
        assertThat(result.ingredients()).extracting(ProductionIngredientResponseDTO::ingredientName)
                .containsExactly("Carne picada", "Cebolla");
        assertThat(result.ingredients().get(0).expectedQuantityGrams()).isEqualByComparingTo("20000.00");
        assertThat(result.ingredients().get(0).actualQuantityGrams()).isEqualByComparingTo("20000.00");
        assertThat(result.ingredients().get(0).costPerGramSnapshot()).isEqualByComparingTo("12.500000");
        assertThat(result.expectedIngredientCost()).isEqualByComparingTo("265000.00");
        assertThat(result.actualIngredientCost()).isEqualByComparingTo("265000.00");
        assertThat(result.standardUnitsPerHour()).isEqualByComparingTo("33.33");
        assertThat(result.actualUnitsPerHour()).isNull();
        assertThat(result.finalizedAt()).isNull();

        meat.setCostPerKilogram(new BigDecimal("20000.000"));
        ingredientRepository.saveAndFlush(meat);
        assertThat(service.getProductionById(result.id()).ingredients().get(0)
                .costPerGramSnapshot()).isEqualByComparingTo("12.500000");
    }

    @Test
    void editsConsumptionAddsExtraAndCalculatesRealMetrics() {
        Ingredient pepper = ingredient("Morrón", "5000.00", "3000.000");
        ProductionResponseDTO draft = createDraft(100);

        service.updateIngredientConsumption(draft.id(),
                new ProductionIngredientUpdateDTO(meat.getId(), new BigDecimal("8400")));
        ProductionResponseDTO withExtra = service.addExtraIngredient(draft.id(),
                new ProductionIngredientUpdateDTO(pepper.getId(), new BigDecimal("250")));
        ProductionResponseDTO result = service.updateProduction(draft.id(),
                new ProductionUpdateDTO(95, 195, 2, 5, " Tapas rotas ", " Medida "));

        assertThat(withExtra.ingredients()).hasSize(3);
        ProductionIngredientResponseDTO extra = withExtra.ingredients().stream()
                .filter(item -> item.ingredientId().equals(pepper.getId())).findFirst().orElseThrow();
        assertThat(extra.expectedQuantityGrams()).isEqualByComparingTo("0.00");
        assertThat(extra.differencePercentage()).isNull();
        assertThat(result.wasteReason()).isEqualTo("Tapas rotas");
        assertThat(result.actualUnitsPerHour()).isEqualByComparingTo("29.23");
        assertThat(result.productivityVariationPercentage()).isEqualByComparingTo("-12.30");
        assertThat(result.actualIngredientCostPerUnit()).isEqualByComparingTo("1176.32");
    }

    @Test
    void finalizesAtomicallyDefaultsExpectedConsumptionAndAddsFinishedStock() {
        stockRepository.saveAndFlush(Stock.builder().varietyId(carne.getVarietyId())
                .productionDate(DATE).totalStock(80).availableStock(80).active(1).build());
        ProductionResponseDTO draft = createDraft(100);
        service.updateIngredientConsumption(draft.id(),
                new ProductionIngredientUpdateDTO(meat.getId(), new BigDecimal("8400")));
        service.updateProduction(draft.id(), new ProductionUpdateDTO(95, 195, 2, 5, "Tapas", null));

        ProductionResponseDTO result = service.finalizeProduction(draft.id());

        assertThat(result.status()).isEqualTo(ProductionStatus.FINALIZED);
        assertThat(result.finalizedAt()).isNotNull();
        Production storedProduction = productionRepository.findById(result.id()).orElseThrow();
        assertThat(storedProduction.getLaborHourlyCostSnapshot())
                .isEqualByComparingTo("5000.00");
        assertThat(storedProduction.getEnergyPercentageSnapshot())
                .isEqualByComparingTo("6.00");
        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow()
                .getCurrentStockGrams()).isEqualByComparingTo("16600.00");
        assertThat(ingredientRepository.findById(onion.getId()).orElseThrow()
                .getCurrentStockGrams()).isEqualByComparingTo("11000.00");
        Stock stock = stockRepository.findByVarietyIdAndActive(carne.getVarietyId(), 1).orElseThrow();
        assertThat(stock.getTotalStock()).isEqualTo(175);
        assertThat(stock.getAvailableStock()).isEqualTo(175);
        assertThatThrownBy(() -> service.finalizeProduction(draft.id()))
                .isInstanceOf(ProductionAlreadyFinalizedException.class);
        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow()
                .getCurrentStockGrams()).isEqualByComparingTo("16600.00");
    }

    @Test
    void insufficientIngredientOrStockFailureRollsBackTheWholeFinalization() {
        ProductionResponseDTO insufficient = createDraft(400);
        service.updateProduction(insufficient.id(),
                new ProductionUpdateDTO(390, null, null, 0, null, null));
        assertThatThrownBy(() -> service.finalizeProduction(insufficient.id()))
                .isInstanceOf(InsufficientIngredientStockException.class);
        assertThat(service.getProductionById(insufficient.id()).status()).isEqualTo(ProductionStatus.DRAFT);
        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow()
                .getCurrentStockGrams()).isEqualByComparingTo("25000.00");

        ProductionResponseDTO stockFailure = createDraft(100);
        service.updateProduction(stockFailure.id(),
                new ProductionUpdateDTO(95, null, null, 0, null, null));
        carne.setActive(0);
        varietyRepository.saveAndFlush(carne);
        assertThatThrownBy(() -> service.finalizeProduction(stockFailure.id()))
                .isInstanceOf(InactiveVarietyException.class);
        assertThat(service.getProductionById(stockFailure.id()).status()).isEqualTo(ProductionStatus.DRAFT);
        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow()
                .getCurrentStockGrams()).isEqualByComparingTo("25000.00");
    }

    @Test
    void supportsMissingStandardProcessAndDraftCancellationWithoutStockChanges() {
        processRepository.deleteAll();
        ProductionResponseDTO draft = createDraft(100);
        assertThat(draft.processId()).isNull();
        assertThat(draft.standardUnitsPerHour()).isNull();

        ProductionResponseDTO canceled = service.cancelProduction(draft.id());
        assertThat(canceled.status()).isEqualTo(ProductionStatus.CANCELED);
        assertThatThrownBy(() -> service.finalizeProduction(draft.id()))
                .isInstanceOf(InvalidProductionStateException.class)
                .hasMessage("Una producción cancelada no puede finalizarse.");
        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow()
                .getCurrentStockGrams()).isEqualByComparingTo("25000.00");
        assertThat(stockRepository.findByVarietyId(carne.getVarietyId())).isEmpty();
    }

    @Test
    void requiresActiveRecipeAndFinalUnits() {
        recipeRepository.findByVarietyVarietyIdAndActiveTrue(carne.getVarietyId())
                .ifPresent(recipe -> { recipe.setActive(false); recipeRepository.saveAndFlush(recipe); });
        assertThatThrownBy(() -> createDraft(100)).isInstanceOf(ActiveRecipeRequiredException.class);

        Recipe recipe = recipeRepository.findTopByVarietyVarietyIdOrderByVersionDesc(carne.getVarietyId())
                .orElseThrow();
        recipe.setActive(true);
        recipeRepository.saveAndFlush(recipe);
        ProductionResponseDTO draft = createDraft(100);
        assertThatThrownBy(() -> service.finalizeProduction(draft.id()))
                .isInstanceOf(InvalidProductionStateException.class)
                .hasMessage("La cantidad final debe informarse antes de finalizar.");
    }

    @Test
    void listsFiltersAndExposesAuthenticatedHttpContract() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        String body = """
                {"varietyId":%d,"productionDate":"2026-09-10","plannedUnits":100,"notes":null}
                """.formatted(carne.getVarietyId());
        String json = mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asLong();

        mockMvc.perform(put(BASE + "/" + id).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalUnits\":95,\"totalMinutes\":195,\"peopleCount\":2,\"wasteUnits\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalUnits").value(95));
        mockMvc.perform(get(BASE + "/status").param("status", "DRAFT").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(BASE + "/date-range").param("from", "2026-09-01")
                        .param("to", "2026-09-10").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(post(BASE + "/" + id + "/finalize").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZED"));
        mockMvc.perform(patch(BASE + "/" + id + "/ingredients").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientId\":" + meat.getId() + ",\"actualQuantityGrams\":1}"))
                .andExpect(status().isConflict());
        assertThat(service.getProductions(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    private ProductionResponseDTO createDraft(int units) {
        return service.createProduction(new ProductionCreateRequestDTO(
                carne.getVarietyId(), DATE, units, null));
    }

    private Ingredient ingredient(String name, String stock, String cost) {
        return ingredientRepository.saveAndFlush(Ingredient.builder().name(name)
                .currentStockGrams(new BigDecimal(stock))
                .minimumStockGrams(new BigDecimal("1000.00"))
                .costPerKilogram(new BigDecimal(cost)).active(true).build());
    }

    private void createRecipe() {
        Recipe recipe = Recipe.builder().variety(carne).version(1).baseYieldUnits(100)
                .notes("v1").active(true).build();
        recipe.addIngredient(RecipeIngredient.builder().ingredient(meat)
                .quantityGrams(new BigDecimal("8000.00")).build());
        recipe.addIngredient(RecipeIngredient.builder().ingredient(onion)
                .quantityGrams(new BigDecimal("4000.00")).build());
        recipeRepository.saveAndFlush(recipe);
    }

    private void createProcess() {
        ProductionProcess process = ProductionProcess.builder().variety(carne).version(1)
                .referenceYieldUnits(100).active(true).build();
        process.addStep(ProductionProcessStep.builder().stepOrder(1).name("Preparar")
                .estimatedMinutes(120).requiredPeople(2).timeType(ProcessTimeType.ACTIVE).build());
        process.addStep(ProductionProcessStep.builder().stepOrder(2).name("Enfriar")
                .estimatedMinutes(60).requiredPeople(0).timeType(ProcessTimeType.WAITING).build());
        processRepository.saveAndFlush(process);
    }
}
