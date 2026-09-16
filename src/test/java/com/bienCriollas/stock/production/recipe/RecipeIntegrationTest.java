package com.bienCriollas.stock.production.recipe;

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

import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.exception.IngredientNotFoundException;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;
import com.bienCriollas.stock.production.recipe.dto.*;
import com.bienCriollas.stock.production.recipe.exception.*;
import com.bienCriollas.stock.production.recipe.interfaces.IRecipeService;
import com.bienCriollas.stock.production.recipe.repository.RecipeRepository;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.variety.exception.VarietyNotFoundException;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recipes-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.datasource.hikari.maximum-pool-size=16"
})
@AutoConfigureMockMvc
class RecipeIntegrationTest {

    private static final String BASE = "/api/v1/recipes";
    private static final PageRequest PAGE = PageRequest.of(0, 20);

    @Autowired private IRecipeService service;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private EmpanadaVarietyRepository varietyRepository;
    @Autowired private MockMvc mockMvc;

    private EmpanadaVariety carne;
    private Ingredient meat;
    private Ingredient onion;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        varietyRepository.deleteAll();
        carne = variety("Carne");
        meat = ingredient("Carne picada", "25000", "12500", true);
        onion = ingredient("Cebolla", "6000", "1350", true);
    }

    @Test
    void createsFirstRecipeAndCalculatesCurrentEstimatedCostsWithoutChangingStock() {
        BigDecimal meatStock = meat.getCurrentStockGrams();
        BigDecimal onionStock = onion.getCurrentStockGrams();

        RecipeResponseDTO result = service.createRecipe(request(carne.getVarietyId(), 100,
                "  Receta estándar  ", item(meat, "8000"), item(onion, "4000")));

        assertThat(result.version()).isEqualTo(1);
        assertThat(result.active()).isTrue();
        assertThat(result.notes()).isEqualTo("Receta estándar");
        assertThat(result.varietyId()).isEqualTo(carne.getVarietyId());
        assertThat(result.varietyName()).isEqualTo("Carne");
        assertThat(result.ingredients()).extracting(RecipeIngredientResponseDTO::ingredientName)
                .containsExactly("Carne picada", "Cebolla");
        assertThat(result.ingredients().get(0).costPerGram()).isEqualByComparingTo("12.5");
        assertThat(result.ingredients().get(0).estimatedCost()).isEqualByComparingTo("100000");
        assertThat(result.ingredients().get(1).estimatedCost()).isEqualByComparingTo("5400");
        assertThat(result.estimatedTotalCost()).isEqualByComparingTo("105400");
        assertThat(result.estimatedCostPerUnit()).isEqualByComparingTo("1054");
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isEqualTo(result.createdAt());

        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow().getCurrentStockGrams())
                .isEqualByComparingTo(meatStock);
        assertThat(ingredientRepository.findById(onion.getId()).orElseThrow().getCurrentStockGrams())
                .isEqualByComparingTo(onionStock);
    }

    @Test
    void createsImmutableVersionsAndKeepsExactlyOneActive() {
        RecipeResponseDTO version1 = createRecipe();
        RecipeResponseDTO version2 = service.createNewVersion(version1.id(),
                versionRequest(120, "Menos cebolla", item(meat, "8000"), item(onion, "3500")));

        assertThat(version2.id()).isNotEqualTo(version1.id());
        assertThat(version2.version()).isEqualTo(2);
        assertThat(version2.active()).isTrue();
        assertThat(service.getRecipeById(version1.id()).active()).isFalse();
        assertThat(service.getRecipeById(version1.id()).baseYieldUnits()).isEqualTo(100);
        assertThat(service.getRecipeById(version1.id()).ingredients().get(1).quantityGrams())
                .isEqualByComparingTo("4000");
        assertThat(service.getRecipeHistory(carne.getVarietyId()))
                .extracting(RecipeResponseDTO::version).containsExactly(2, 1);
        assertThat(recipeRepository.findByActiveTrue(PAGE).getTotalElements()).isEqualTo(1);
    }

    @Test
    void canVersionFromHistoricalIdWhileIncrementingLatestVersion() {
        RecipeResponseDTO first = createRecipe();
        RecipeResponseDTO second = service.createNewVersion(first.id(),
                versionRequest(100, "v2", item(meat, "7000")));
        RecipeResponseDTO third = service.createNewVersion(first.id(),
                versionRequest(100, "v3", item(meat, "6500")));

        assertThat(second.version()).isEqualTo(2);
        assertThat(third.version()).isEqualTo(3);
        assertThat(service.getActiveRecipeByVariety(carne.getVarietyId()).id())
                .isEqualTo(third.id());
        assertThat(service.getRecipeHistory(carne.getVarietyId()))
                .extracting(RecipeResponseDTO::active)
                .containsExactly(true, false, false);
    }

    @Test
    void currentIngredientPriceUpdatesHistoricalEstimatedCosts() {
        RecipeResponseDTO created = createRecipe();
        BigDecimal stockBefore = meat.getCurrentStockGrams();
        meat.setCostPerKilogram(new BigDecimal("15000"));
        ingredientRepository.saveAndFlush(meat);

        RecipeResponseDTO currentEstimate = service.getRecipeById(created.id());
        assertThat(currentEstimate.ingredients().get(0).costPerGram())
                .isEqualByComparingTo("15");
        assertThat(currentEstimate.estimatedTotalCost()).isEqualByComparingTo("125400");
        assertThat(meat.getCurrentStockGrams()).isEqualByComparingTo(stockBefore);
    }

    @Test
    void calculatesScaledRequirementsAvailabilityAndCostWithoutChangingStock() {
        RecipeResponseDTO recipe = createRecipe();
        BigDecimal meatBefore = meat.getCurrentStockGrams();
        BigDecimal onionBefore = onion.getCurrentStockGrams();

        RecipeCalculationResponseDTO result = service.calculateRecipe(recipe.id(), 250);

        assertThat(result.scaleFactor()).isEqualByComparingTo("2.5");
        assertThat(result.requestedUnits()).isEqualTo(250);
        RecipeCalculatedIngredientDTO calculatedMeat = result.ingredients().get(0);
        assertThat(calculatedMeat.requiredQuantityGrams()).isEqualByComparingTo("20000");
        assertThat(calculatedMeat.enoughStock()).isTrue();
        assertThat(calculatedMeat.missingGrams()).isZero();
        assertThat(calculatedMeat.estimatedCost()).isEqualByComparingTo("250000");
        RecipeCalculatedIngredientDTO calculatedOnion = result.ingredients().get(1);
        assertThat(calculatedOnion.requiredQuantityGrams()).isEqualByComparingTo("10000");
        assertThat(calculatedOnion.enoughStock()).isFalse();
        assertThat(calculatedOnion.missingGrams()).isEqualByComparingTo("4000");
        assertThat(result.estimatedTotalCost()).isEqualByComparingTo("263500");
        assertThat(result.estimatedCostPerUnit()).isEqualByComparingTo("1054");

        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow().getCurrentStockGrams())
                .isEqualByComparingTo(meatBefore);
        assertThat(ingredientRepository.findById(onion.getId()).orElseThrow().getCurrentStockGrams())
                .isEqualByComparingTo(onionBefore);
    }

    @Test
    void followsRequiredRoundingRulesWhenScaling() {
        RecipeResponseDTO recipe = service.createRecipe(request(
                carne.getVarietyId(), 3, null, item(meat, "1")));
        RecipeCalculationResponseDTO result = service.calculateRecipe(recipe.id(), 1);

        assertThat(result.scaleFactor()).isEqualByComparingTo("0.333333");
        assertThat(result.ingredients().get(0).requiredQuantityGrams())
                .isEqualByComparingTo("0.33");
    }

    @Test
    void rejectsDuplicateMissingAndInactiveIngredients() {
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 100, null,
                item(meat, "1"), item(meat, "2"))))
                .isInstanceOf(RecipeIngredientDuplicatedException.class)
                .hasMessage("El ingrediente Carne picada está repetido dentro de la receta.");

        Ingredient inactive = ingredient("Morrón", "10", "2000", false);
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 100, null,
                item(inactive, "1"))))
                .isInstanceOf(InactiveIngredientForRecipeException.class)
                .hasMessageContaining("Morrón");

        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 100, null,
                new RecipeIngredientRequestDTO(Long.MAX_VALUE, BigDecimal.ONE))))
                .isInstanceOf(IngredientNotFoundException.class);
    }

    @Test
    void failedNewVersionKeepsCurrentRecipeActiveAndLeavesStockUntouched() {
        RecipeResponseDTO current = createRecipe();
        BigDecimal stockBefore = meat.getCurrentStockGrams();
        Ingredient inactive = ingredient("Morrón", "10", "2000", false);

        assertThatThrownBy(() -> service.createNewVersion(current.id(),
                versionRequest(100, null, item(meat, "7000"), item(inactive, "500"))))
                .isInstanceOf(InactiveIngredientForRecipeException.class);

        assertThat(service.getRecipeById(current.id()).active()).isTrue();
        assertThat(service.getRecipeHistory(carne.getVarietyId())).hasSize(1);
        assertThat(ingredientRepository.findById(meat.getId()).orElseThrow().getCurrentStockGrams())
                .isEqualByComparingTo(stockBefore);
    }

    @Test
    void rejectsSecondActiveRecipeAndMissingVariety() {
        createRecipe();
        assertThatThrownBy(this::createRecipe)
                .isInstanceOf(RecipeAlreadyExistsException.class)
                .hasMessage("La variedad Carne ya tiene una receta activa.");
        assertThatThrownBy(() -> service.createRecipe(request(Long.MAX_VALUE, 100, null,
                item(meat, "1"))))
                .isInstanceOf(VarietyNotFoundException.class);
    }

    @Test
    void validatesYieldIngredientsQuantityNotesIdsAndCalculationQuantity() {
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 0, null,
                item(meat, "1")))).isInstanceOf(InvalidRecipeYieldException.class);
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 1, null)))
                .isInstanceOf(RecipeWithoutIngredientsException.class);
        assertThatThrownBy(() -> service.createRecipe(null))
                .isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 1,
                "x".repeat(501), item(meat, "1"))))
                .isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 1, null,
                item(meat, "0")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> service.createRecipe(request(carne.getVarietyId(), 1, null,
                item(meat, "0.001")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> service.getRecipeById(-1L))
                .isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> service.getRecipeById(Long.MAX_VALUE))
                .isInstanceOf(RecipeNotFoundException.class);
        RecipeResponseDTO recipe = createRecipe();
        assertThatThrownBy(() -> service.calculateRecipe(recipe.id(), 0))
                .isInstanceOf(InvalidRecipeYieldException.class);
    }

    @Test
    void listsActiveInactiveHistoryAndValidatesSort() {
        RecipeResponseDTO first = createRecipe();
        service.createNewVersion(first.id(), versionRequest(100, "v2", item(meat, "7000")));
        EmpanadaVariety chicken = variety("Pollo");
        service.createRecipe(request(chicken.getVarietyId(), 50, null, item(onion, "2000")));

        assertThat(service.getActiveRecipes(PageRequest.of(0, 1)).getTotalElements()).isEqualTo(2);
        assertThat(service.getRecipesByStatus(false, PAGE).getTotalElements()).isEqualTo(1);
        assertThat(service.getRecipeHistory(carne.getVarietyId())).hasSize(2);
        assertThatThrownBy(() -> service.getRecipesByStatus(null, PAGE))
                .isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> service.getActiveRecipes(PageRequest.of(0, 10)
                .withSort(org.springframework.data.domain.Sort.by("estimatedTotalCost"))))
                .isInstanceOf(InvalidRecipeException.class);
        assertThat(service.getActiveRecipes(PageRequest.of(0, 10)
                .withSort(org.springframework.data.domain.Sort.by("varietyName"))).getContent())
                .extracting(RecipeResponseDTO::varietyName).containsExactly("Carne", "Pollo");
    }

    @Test
    void concurrentFirstRecipesKeepOneActiveRecipe() throws Exception {
        List<Boolean> outcomes = concurrently(8, () -> {
            try {
                createRecipe();
                return true;
            } catch (RecipeAlreadyExistsException expected) {
                return false;
            }
        });

        assertThat(outcomes.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(recipeRepository.findByActiveTrue(PAGE).getTotalElements()).isEqualTo(1);
    }

    @Test
    void concurrentVersionsAreSequentialAndKeepOneActiveRecipe() throws Exception {
        RecipeResponseDTO first = createRecipe();
        List<Integer> versions = concurrently(6, () -> service.createNewVersion(
                first.id(), versionRequest(100, null, item(meat, "7000"))).version());

        assertThat(versions).containsExactlyInAnyOrder(2, 3, 4, 5, 6, 7);
        assertThat(recipeRepository.findByActiveTrue(PAGE).getTotalElements()).isEqualTo(1);
        assertThat(service.getRecipeHistory(carne.getVarietyId()))
                .extracting(RecipeResponseDTO::version)
                .containsExactly(7, 6, 5, 4, 3, 2, 1);
    }

    @Test
    void apiRequiresAuthenticationAndExposesCompleteContract() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        String createBody = body(carne.getVarietyId(), 100, "Receta estándar",
                item(meat, "8000"), item(onion, "4000"));
        String response = mockMvc.perform(post(BASE).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.estimatedTotalCost").value(105400))
                .andReturn().getResponse().getContentAsString();
        long recipeId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        mockMvc.perform(get(BASE).with(jwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(BASE + "/" + recipeId).with(jwt())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/variety/" + carne.getVarietyId()).with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(recipeId));
        mockMvc.perform(get(BASE + "/variety/" + carne.getVarietyId() + "/history").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].version").value(1));
        mockMvc.perform(get(BASE + "/status").param("active", "true").with(jwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(BASE + "/" + recipeId + "/calculate")
                        .param("quantity", "250").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scaleFactor").value(2.5))
                .andExpect(jsonPath("$.ingredients[1].missingGrams").value(4000));

        mockMvc.perform(post(BASE + "/" + recipeId + "/versions").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(versionBody(100, "v2", item(meat, "7000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void apiReturnsStandardErrorsAndRejectsInvalidPayloads() throws Exception {
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get(BASE + "/9223372036854775807").with(jwt()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/status").with(jwt()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/1/calculate").param("quantity", "0").with(jwt()))
                .andExpect(status().isBadRequest());

        createRecipe();
        mockMvc.perform(post(BASE).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(carne.getVarietyId(), 100, null, item(meat, "1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La variedad Carne ya tiene una receta activa."));
    }

    private RecipeResponseDTO createRecipe() {
        return service.createRecipe(request(carne.getVarietyId(), 100, "v1",
                item(meat, "8000"), item(onion, "4000")));
    }

    private RecipeRequestDTO request(
            Long varietyId, Integer yield, String notes, RecipeIngredientRequestDTO... items) {
        return new RecipeRequestDTO(varietyId, yield, notes, List.of(items));
    }

    private RecipeVersionRequestDTO versionRequest(
            Integer yield, String notes, RecipeIngredientRequestDTO... items) {
        return new RecipeVersionRequestDTO(yield, notes, List.of(items));
    }

    private RecipeIngredientRequestDTO item(Ingredient ingredient, String quantity) {
        return new RecipeIngredientRequestDTO(ingredient.getId(), new BigDecimal(quantity));
    }

    private String body(
            Long varietyId, int yield, String notes, RecipeIngredientRequestDTO... items)
            throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                request(varietyId, yield, notes, items));
    }

    private String versionBody(
            int yield, String notes, RecipeIngredientRequestDTO... items) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                versionRequest(yield, notes, items));
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

    private Ingredient ingredient(
            String name, String stock, String costPerKilogram, boolean active) {
        return ingredientRepository.saveAndFlush(Ingredient.builder()
                .name(name)
                .currentStockGrams(new BigDecimal(stock))
                .minimumStockGrams(BigDecimal.ZERO)
                .costPerKilogram(new BigDecimal(costPerKilogram))
                .active(active)
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
