package com.bienCriollas.stock.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;
import com.bienCriollas.stock.production.recipe.dto.RecipeAdditionalCostRequestDTO;
import com.bienCriollas.stock.production.recipe.dto.RecipeIngredientRequestDTO;
import com.bienCriollas.stock.production.recipe.dto.RecipeRequestDTO;
import com.bienCriollas.stock.production.recipe.enums.AdditionalCostCalculationMode;
import com.bienCriollas.stock.production.recipe.enums.AdditionalCostType;
import com.bienCriollas.stock.production.recipe.interfaces.IRecipeService;
import com.bienCriollas.stock.production.recipe.repository.RecipeRepository;
import com.bienCriollas.stock.stock.dto.StockActualResponseDTO;
import com.bienCriollas.stock.stock.dto.StockSummaryResponseDTO;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.interfaces.IStockService;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;
import com.bienCriollas.stock.waste.dto.EmpanadaLossDTO;
import com.bienCriollas.stock.waste.repository.WasteRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock-valuation-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class StockValuationIntegrationTest {

    private static final String BASE = "/api/v2/stock";

    @Autowired private IStockService stockService;
    @Autowired private IRecipeService recipeService;
    @Autowired private StockRepository stockRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private EmpanadaVarietyRepository varietyRepository;
    @Autowired private WasteRepository wasteRepository;
    @Autowired private MockMvc mockMvc;

    private EmpanadaVariety hamAndCheese;
    private EmpanadaVariety withoutRecipe;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        wasteRepository.deleteAll();
        stockRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        varietyRepository.deleteAll();

        hamAndCheese = variety("Jamon y queso");
        withoutRecipe = variety("Bondiola");
        ingredient = ingredient("Relleno JyQ", "600.000000");
        createRecipe();
        stockRepository.saveAllAndFlush(List.of(
                stock(hamAndCheese.getVarietyId(), 165, 100),
                stock(withoutRecipe.getVarietyId(), 50, 43)));
    }

    @Test
    void returnsCurrentRecipeCostAndKeepsMissingRecipeAsNull() {
        List<StockActualResponseDTO> result = stockService.getAllStockRecords();

        StockActualResponseDTO valued = result.stream()
                .filter(item -> item.varietyId().equals(hamAndCheese.getVarietyId()))
                .findFirst().orElseThrow();
        assertThat(valued.currentUnitCost()).isEqualByComparingTo("618.67");
        assertThat(valued.currentStockValue()).isEqualByComparingTo("61867.00");

        StockActualResponseDTO unavailable = result.stream()
                .filter(item -> item.varietyId().equals(withoutRecipe.getVarietyId()))
                .findFirst().orElseThrow();
        assertThat(unavailable.currentUnitCost()).isNull();
        assertThat(unavailable.currentStockValue()).isNull();
    }

    @Test
    void recalculatesFromCurrentIngredientPriceAndAvailableUnits() {
        ingredient.setCostPerBaseUnit(new BigDecimal("621.330000"));
        ingredientRepository.saveAndFlush(ingredient);

        StockActualResponseDTO repriced = currentStock(hamAndCheese.getVarietyId());
        assertThat(repriced.currentUnitCost()).isEqualByComparingTo("640.00");
        assertThat(repriced.currentStockValue()).isEqualByComparingTo("64000.00");

        stockService.registerLosses(List.of(new EmpanadaLossDTO(
                hamAndCheese.getVarietyId(), 10)));

        StockActualResponseDTO afterLoss = currentStock(hamAndCheese.getVarietyId());
        assertThat(afterLoss.availableStock()).isEqualTo(90);
        assertThat(afterLoss.currentStockValue()).isEqualByComparingTo("57600.00");
    }

    @Test
    void exposesValuedCurrentStockAndSummaryWithoutChangingHistoryContract() throws Exception {
        mockMvc.perform(get(BASE + "/obtener-stock-actual"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE + "/obtener-stock-actual").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id_variedad == "
                        + hamAndCheese.getVarietyId() + ")].costo_unitario_actual")
                        .value(618.67))
                .andExpect(jsonPath("$[?(@.id_variedad == "
                        + hamAndCheese.getVarietyId() + ")].valor_stock_actual")
                        .value(61867.00))
                .andExpect(jsonPath("$[?(@.id_variedad == "
                        + withoutRecipe.getVarietyId() + ")].costo_unitario_actual")
                        .value(contains(nullValue())));

        mockMvc.perform(get(BASE + "/resumen").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_unidades_disponibles").value(143))
                .andExpect(jsonPath("$.valor_total_stock").value(61867.00))
                .andExpect(jsonPath("$.variedades_con_stock").value(2))
                .andExpect(jsonPath("$.variedades_sin_valoracion").value(1));

        mockMvc.perform(get(BASE + "/obtener-variedad/" + hamAndCheese.getVarietyId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].costo_unitario_actual").doesNotExist())
                .andExpect(jsonPath("$[0].valor_stock_actual").doesNotExist());
    }

    @Test
    void reportsZeroValueWhenAValuedVarietyHasNoAvailableUnits() {
        Stock current = stockRepository.findByVarietyIdAndActive(
                hamAndCheese.getVarietyId(), 1).orElseThrow();
        current.setAvailableStock(0);
        stockRepository.saveAndFlush(current);

        StockActualResponseDTO result = currentStock(hamAndCheese.getVarietyId());

        assertThat(result.currentUnitCost()).isEqualByComparingTo("618.67");
        assertThat(result.currentStockValue()).isEqualByComparingTo("0.00");
        StockSummaryResponseDTO summary = stockService.getStockSummary();
        assertThat(summary.varietiesWithStock()).isEqualTo(1);
    }

    private StockActualResponseDTO currentStock(Long varietyId) {
        return stockService.getAllStockRecords().stream()
                .filter(item -> item.varietyId().equals(varietyId))
                .findFirst().orElseThrow();
    }

    private void createRecipe() {
        recipeService.createRecipe(new RecipeRequestDTO(
                hamAndCheese.getVarietyId(),
                100,
                "Receta de prueba",
                List.of(new RecipeIngredientRequestDTO(
                        ingredient.getId(), new BigDecimal("100.0000"))),
                List.of(new RecipeAdditionalCostRequestDTO(
                        AdditionalCostType.PACKAGING,
                        "Descartables",
                        AdditionalCostCalculationMode.PER_UNIT,
                        new BigDecimal("18.670000"),
                        1,
                        null))));
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

    private Ingredient ingredient(String name, String cost) {
        return ingredientRepository.saveAndFlush(Ingredient.builder()
                .name(name)
                .measurementUnit(MeasurementUnit.UNIT)
                .costPerBaseUnit(new BigDecimal(cost))
                .active(true)
                .build());
    }

    private Stock stock(Long varietyId, int total, int available) {
        return Stock.builder()
                .varietyId(varietyId)
                .productionDate(LocalDate.of(2026, 9, 29))
                .totalStock(total)
                .availableStock(available)
                .active(1)
                .build();
    }
}
