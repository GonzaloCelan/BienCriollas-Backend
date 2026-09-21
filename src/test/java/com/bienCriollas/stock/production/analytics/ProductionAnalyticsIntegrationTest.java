package com.bienCriollas.stock.production.analytics;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bienCriollas.stock.production.analytics.dto.*;
import com.bienCriollas.stock.production.analytics.entity.ProductionCostSettings;
import com.bienCriollas.stock.production.analytics.interfaces.IProductionAnalyticsService;
import com.bienCriollas.stock.production.analytics.repository.ProductionCostSettingsRepository;
import com.bienCriollas.stock.production.dto.*;
import com.bienCriollas.stock.production.entity.Production;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;
import com.bienCriollas.stock.production.interfaces.IProductionService;
import com.bienCriollas.stock.production.process.entity.*;
import com.bienCriollas.stock.production.process.enums.ProcessTimeType;
import com.bienCriollas.stock.production.process.repository.ProductionProcessRepository;
import com.bienCriollas.stock.production.recipe.entity.*;
import com.bienCriollas.stock.production.recipe.enums.*;
import com.bienCriollas.stock.production.recipe.repository.RecipeRepository;
import com.bienCriollas.stock.production.repository.*;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:production-analytics-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ProductionAnalyticsIntegrationTest {

    private static final String BASE = "/api/v1/production-analytics";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

    @Autowired private IProductionAnalyticsService analyticsService;
    @Autowired private IProductionService productionService;
    @Autowired private ProductionCostSettingsRepository settingsRepository;
    @Autowired private ProductionIngredientRepository productionIngredientRepository;
    @Autowired private ProductionRepository productionRepository;
    @Autowired private ProductionProcessRepository processRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private EmpanadaVarietyRepository varietyRepository;
    @Autowired private MockMvc mockMvc;

    private EmpanadaVariety carne;
    private Ingredient meat;
    private Ingredient onion;
    private long inefficientProductionId;
    private long efficientProductionId;

    @BeforeEach
    void setUp() {
        productionIngredientRepository.deleteAll();
        productionRepository.deleteAll();
        settingsRepository.deleteAll();
        processRepository.deleteAll();
        recipeRepository.deleteAll();
        stockRepository.deleteAll();
        ingredientRepository.deleteAll();
        varietyRepository.deleteAll();

        settingsRepository.saveAndFlush(ProductionCostSettings.builder()
                .averageHourlyLaborCost(new BigDecimal("5000.00"))
                .energyPercentage(new BigDecimal("6.00")).build());
        carne = varietyRepository.saveAndFlush(EmpanadaVariety.builder()
                .name("Carne").unitPrice(new BigDecimal("1500"))
                .halfDozenPrice(new BigDecimal("8000"))
                .dozenPrice(new BigDecimal("15000")).active(1).build());
        meat = ingredient("Carne picada", "100000.00", "12.500000");
        onion = ingredient("Cebolla", "100000.00", "1.500000");
        createRecipe();
        createProcess();

        inefficientProductionId = createFinalizedProduction(
                100, 180, 2, 5, "Tapas rotas", new BigDecimal("8400.00"));
        efficientProductionId = createFinalizedProduction(
                100, 120, 2, 0, null, new BigDecimal("8000.00"));
        productionService.createProduction(new ProductionCreateRequestDTO(
                carne.getVarietyId(), DATE, 100, "Borrador excluido"));
    }

    @Test
    void calculatesDetailedStandardVsActualCostsAndPerformance() {
        ProductionCostDetailDTO detail =
                analyticsService.getProductionDetail(inefficientProductionId);

        assertThat(detail.expectedIngredientCost()).isEqualByComparingTo("106000.00");
        assertThat(detail.actualIngredientCost()).isEqualByComparingTo("111000.00");
        assertThat(detail.ingredientCostDeviation()).isEqualByComparingTo("5000.00");
        assertThat(detail.standardUnitsPerHour()).isEqualByComparingTo("33.33");
        assertThat(detail.actualUnitsPerHour()).isEqualByComparingTo("33.33");
        assertThat(detail.productivityVariationPercentage()).isEqualByComparingTo("0.00");
        assertThat(detail.standardPersonHours()).isEqualByComparingTo("4.00");
        assertThat(detail.actualPersonHours()).isEqualByComparingTo("6.00");
        assertThat(detail.standardUnitsPerPersonHour()).isEqualByComparingTo("25.00");
        assertThat(detail.actualUnitsPerPersonHour()).isEqualByComparingTo("16.67");
        assertThat(detail.laborProductivityVariationPercentage()).isEqualByComparingTo("-33.33");
        assertThat(detail.laborHourlyCostSnapshot()).isEqualByComparingTo("5000.00");
        assertThat(detail.expectedPersonHoursForActualOutput()).isEqualByComparingTo("4.00");
        assertThat(detail.expectedLaborCostForActualOutput()).isEqualByComparingTo("20000.00");
        assertThat(detail.actualLaborCost()).isEqualByComparingTo("30000.00");
        assertThat(detail.laborInefficiencyCost()).isEqualByComparingTo("10000.00");
        assertThat(detail.energyCost()).isEqualByComparingTo("8460.00");
        assertThat(detail.standardTotalCost()).isEqualByComparingTo("133560.00");
        assertThat(detail.actualTotalCost()).isEqualByComparingTo("149460.00");
        assertThat(detail.standardCostPerUnit()).isEqualByComparingTo("1335.60");
        assertThat(detail.actualCostPerUnit()).isEqualByComparingTo("1494.60");
        assertThat(detail.totalCostDeviation()).isEqualByComparingTo("15900.00");
        assertThat(detail.totalCostDeviationPerUnit()).isEqualByComparingTo("159.00");
        assertThat(detail.wastePercentage()).isEqualByComparingTo("5.00");
        assertThat(detail.estimatedWasteCost()).isEqualByComparingTo("7473.00");
        assertThat(detail.performanceStatus()).isEqualTo("CRITICAL");
    }

    @Test
    void aggregatesOnlyFinalizedProductionsAcrossAllDashboardViews() {
        CostPerformanceSummaryDTO summary = analyticsService.getSummary(DATE, DATE);
        assertThat(summary.totalProductions()).isEqualTo(2);
        assertThat(summary.totalUnitsProduced()).isEqualTo(200);
        assertThat(summary.totalWasteUnits()).isEqualTo(5);
        assertThat(summary.totalIngredientCost()).isEqualByComparingTo("217000.00");
        assertThat(summary.totalLaborCost()).isEqualByComparingTo("50000.00");
        assertThat(summary.totalEnergyCost()).isEqualByComparingTo("16020.00");
        assertThat(summary.totalProductionCost()).isEqualByComparingTo("283020.00");
        assertThat(summary.averageCostPerUnit()).isEqualByComparingTo("1415.10");
        assertThat(summary.totalPersonHours()).isEqualByComparingTo("10.00");
        assertThat(summary.averageUnitsPerPersonHour()).isEqualByComparingTo("20.00");
        assertThat(summary.averageProductivityVariationPercentage())
                .isEqualByComparingTo("-16.67");
        assertThat(summary.totalLaborInefficiencyCost()).isEqualByComparingTo("10000.00");
        assertThat(summary.totalEstimatedWasteCost()).isEqualByComparingTo("7473.00");
        assertThat(summary.efficientProductions()).isEqualTo(1);
        assertThat(summary.inefficientProductions()).isEqualTo(1);

        LaborPerformanceSummaryDTO labor = analyticsService.getLaborPerformance(DATE, DATE);
        assertThat(labor.totalPersonHours()).isEqualByComparingTo("10.00");
        assertThat(labor.expectedPersonHours()).isEqualByComparingTo("8.00");
        assertThat(labor.extraPersonHours()).isEqualByComparingTo("2.00");
        assertThat(labor.averageUnitsPerPersonHour()).isEqualByComparingTo("20.00");
        assertThat(labor.standardUnitsPerPersonHour()).isEqualByComparingTo("25.00");
        assertThat(labor.productivityVariationPercentage()).isEqualByComparingTo("-20.00");
        assertThat(labor.actualLaborCost()).isEqualByComparingTo("50000.00");
        assertThat(labor.expectedLaborCost()).isEqualByComparingTo("40000.00");
        assertThat(labor.laborInefficiencyCost()).isEqualByComparingTo("10000.00");

        VarietyPerformanceDTO variety = analyticsService
                .getVarietyPerformance(carne.getVarietyId(), DATE, DATE);
        assertThat(variety.productionCount()).isEqualTo(2);
        assertThat(variety.averageCostPerUnit()).isEqualByComparingTo("1415.10");
        assertThat(variety.averageUnitsPerHour()).isEqualByComparingTo("40.00");
        assertThat(variety.averageUnitsPerPersonHour()).isEqualByComparingTo("20.00");
    }

    @Test
    void reportsIngredientWasteAndRankingsUsingSnapshots() {
        assertThat(analyticsService.getIngredientDeviations(DATE, DATE))
                .first().satisfies(deviation -> {
                    assertThat(deviation.ingredientName()).isEqualTo("Carne picada");
                    assertThat(deviation.measurementUnit()).isEqualTo(MeasurementUnit.GRAM);
                    assertThat(deviation.totalExpectedQuantity()).isEqualByComparingTo("16000.00");
                    assertThat(deviation.totalActualQuantity()).isEqualByComparingTo("16400.00");
                    assertThat(deviation.differencePercentage()).isEqualByComparingTo("2.50");
                    assertThat(deviation.additionalCost()).isEqualByComparingTo("5000.00");
                });

        assertThat(analyticsService.getWasteSummary(DATE, DATE))
                .containsExactly(new WasteReasonSummaryDTO(
                        "Tapas rotas", 5, new BigDecimal("100.00"),
                        new BigDecimal("7473.00")));
        assertThat(analyticsService.getBestProductions(DATE, DATE, 1).get(0).productionId())
                .isEqualTo(efficientProductionId);
        assertThat(analyticsService.getWorstProductions(DATE, DATE, 1).get(0).productionId())
                .isEqualTo(inefficientProductionId);

        meat.setCostPerBaseUnit(new BigDecimal("99.999000"));
        ingredientRepository.saveAndFlush(meat);
        assertThat(analyticsService.getProductionDetail(inefficientProductionId)
                .actualIngredientCost()).isEqualByComparingTo("111000.00");
    }

    @Test
    void comparesRecipeStandardAdditionalCostsWithActualProductionCosts() {
        Recipe recipe = recipeRepository
                .findByVarietyVarietyIdAndActiveTrue(carne.getVarietyId()).orElseThrow();
        recipe.setAdditionalCosts(new ArrayList<>());
        recipe.addAdditionalCost(additionalCost(AdditionalCostType.LABOR,
                AdditionalCostCalculationMode.FIXED_TOTAL, "Mano de obra", "25000", 1));
        recipe.addAdditionalCost(additionalCost(AdditionalCostType.PACKAGING,
                AdditionalCostCalculationMode.PER_UNIT, "Descartables", "10", 2));
        recipe.addAdditionalCost(additionalCost(AdditionalCostType.ENERGY,
                AdditionalCostCalculationMode.PERCENTAGE, "Energía", "7", 3));
        recipeRepository.saveAndFlush(recipe);

        long productionId = createFinalizedProduction(
                100, 180, 2, 0, null, new BigDecimal("8000.00"));
        ProductionCostDetailDTO detail = analyticsService.getProductionDetail(productionId);

        assertThat(productionRepository.findById(productionId).orElseThrow()
                .getEnergyPercentageSnapshot()).isEqualByComparingTo("7.00");

        assertThat(detail.expectedLaborCostForActualOutput()).isEqualByComparingTo("25000.00");
        assertThat(detail.actualLaborCost()).isEqualByComparingTo("30000.00");
        assertThat(detail.laborInefficiencyCost()).isEqualByComparingTo("5000.00");
        assertThat(detail.expectedPackagingCost()).isEqualByComparingTo("1000.00");
        assertThat(detail.actualPackagingCost()).isEqualByComparingTo("1000.00");
        assertThat(detail.expectedEnergyCost()).isEqualByComparingTo("9240.00");
        assertThat(detail.energyCost()).isEqualByComparingTo("9590.00");
        assertThat(detail.standardTotalCost()).isEqualByComparingTo("141240.00");
        assertThat(detail.actualTotalCost()).isEqualByComparingTo("146590.00");
    }

    @Test
    void exposesAuthenticatedHttpContractAndValidatesSettingsAndRanges() throws Exception {
        mockMvc.perform(get(BASE + "/summary")
                        .param("from", "2026-09-10").param("to", "2026-09-10"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/summary").with(jwt())
                        .param("from", "2026-09-10").param("to", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductions").value(2))
                .andExpect(jsonPath("$.totalLaborInefficiencyCost").value(10000));
        mockMvc.perform(get(BASE + "/productions/" + inefficientProductionId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.performanceStatus").value("CRITICAL"));
        mockMvc.perform(get(BASE + "/summary").with(jwt())
                        .param("from", "2026-09-11").param("to", "2026-09-10"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE + "/settings").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"averageHourlyLaborCost\":6500,\"energyPercentage\":7.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageHourlyLaborCost").value(6500))
                .andExpect(jsonPath("$.energyPercentage").value(7.5));
        mockMvc.perform(put(BASE + "/settings").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"averageHourlyLaborCost\":0,\"energyPercentage\":101}"))
                .andExpect(status().isBadRequest());
    }

    private long createFinalizedProduction(
            int finalUnits,
            int minutes,
            int people,
            int waste,
            String wasteReason,
            BigDecimal actualMeat) {
        ProductionResponseDTO draft = productionService.createProduction(
                new ProductionCreateRequestDTO(carne.getVarietyId(), DATE, 100, null));
        productionService.updateIngredientConsumption(draft.id(),
                new ProductionIngredientUpdateDTO(meat.getId(), actualMeat));
        productionService.updateProduction(draft.id(), new ProductionUpdateDTO(
                finalUnits, minutes, people, waste, wasteReason, null));
        productionService.finalizeProduction(draft.id());
        Production stored = productionRepository.findById(draft.id()).orElseThrow();
        assertThat(stored.getLaborHourlyCostSnapshot()).isEqualByComparingTo("5000.00");
        assertThat(stored.getEnergyPercentageSnapshot()).isNotNull();
        return draft.id();
    }

    private Ingredient ingredient(String name, String stock, String cost) {
        return ingredientRepository.saveAndFlush(Ingredient.builder().name(name)
                .measurementUnit(MeasurementUnit.GRAM)
                .currentStock(new BigDecimal(stock))
                .minimumStock(BigDecimal.ZERO.setScale(2))
                .costPerBaseUnit(new BigDecimal(cost)).active(true).build());
    }

    private void createRecipe() {
        Recipe recipe = Recipe.builder().variety(carne).version(1).baseYieldUnits(100)
                .notes("v1").active(true).build();
        recipe.addIngredient(RecipeIngredient.builder().ingredient(meat)
                .quantity(new BigDecimal("8000.00")).build());
        recipe.addIngredient(RecipeIngredient.builder().ingredient(onion)
                .quantity(new BigDecimal("4000.00")).build());
        recipeRepository.saveAndFlush(recipe);
    }

    private RecipeAdditionalCost additionalCost(
            AdditionalCostType type, AdditionalCostCalculationMode mode,
            String name, String value, int order) {
        return RecipeAdditionalCost.builder().costType(type).calculationMode(mode)
                .name(name).value(new BigDecimal(value)).sortOrder(order).active(true).build();
    }

    private void createProcess() {
        ProductionProcess process = ProductionProcess.builder().variety(carne).version(1)
                .referenceYieldUnits(100).active(true).build();
        process.addStep(ProductionProcessStep.builder().stepOrder(1).name("Preparar")
                .estimatedMinutes(120).requiredPeople(2)
                .timeType(ProcessTimeType.ACTIVE).build());
        process.addStep(ProductionProcessStep.builder().stepOrder(2).name("Enfriar")
                .estimatedMinutes(60).requiredPeople(0)
                .timeType(ProcessTimeType.WAITING).build());
        processRepository.saveAndFlush(process);
    }
}
