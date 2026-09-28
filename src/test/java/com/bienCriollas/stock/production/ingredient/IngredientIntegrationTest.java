package com.bienCriollas.stock.production.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bienCriollas.stock.production.ingredient.dto.IngredientCostUpdateDTO;
import com.bienCriollas.stock.production.ingredient.dto.IngredientRequestDTO;
import com.bienCriollas.stock.production.ingredient.dto.IngredientResponseDTO;
import com.bienCriollas.stock.production.ingredient.dto.IngredientSummaryDTO;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;
import com.bienCriollas.stock.production.ingredient.enums.ReferencePriceUnit;
import com.bienCriollas.stock.production.ingredient.exception.IngredientAlreadyExistsException;
import com.bienCriollas.stock.production.ingredient.exception.InvalidIngredientException;
import com.bienCriollas.stock.production.ingredient.interfaces.IIngredientService;
import com.bienCriollas.stock.production.ingredient.repository.IngredientRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ingredients-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class IngredientIntegrationTest {

    private static final String BASE = "/api/v1/ingredients";

    @Autowired private IIngredientService service;
    @Autowired private IngredientRepository repository;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void clearIngredients() {
        repository.deleteAll();
    }

    @Test
    void createsGramIngredientWithoutStockAndCalculatesKilogramReference() {
        IngredientResponseDTO result = service.createIngredient(request(
                "  Carne molida  ", MeasurementUnit.GRAM, "Bolsa", "1000", "9999"));

        assertThat(result.name()).isEqualTo("Carne molida");
        assertThat(result.costPerBaseUnit()).isEqualByComparingTo("9.999");
        assertThat(result.referencePrice()).isEqualByComparingTo("9999.00");
        assertThat(result.referencePriceUnit()).isEqualTo(ReferencePriceUnit.KG);
        assertThat(result.active()).isTrue();
        assertThat(result.purchaseDataComplete()).isTrue();

        Ingredient stored = repository.findById(result.id()).orElseThrow();
        assertThat(stored.getCurrentStock()).isZero();
        assertThat(stored.getMinimumStock()).isZero();
    }

    @Test
    void createsUnitAndMilliliterIngredientsWithUsefulReferencePrices() {
        IngredientResponseDTO egg = service.createIngredient(request(
                "Huevo", MeasurementUnit.UNIT, "Maple", "30", "6900"));
        IngredientResponseDTO oil = service.createIngredient(request(
                "Aceite", MeasurementUnit.MILLILITER, "Botella", "900", "2740"));

        assertThat(egg.costPerBaseUnit()).isEqualByComparingTo("230");
        assertThat(egg.referencePrice()).isEqualByComparingTo("230.00");
        assertThat(egg.referencePriceUnit()).isEqualTo(ReferencePriceUnit.UNIT);
        assertThat(oil.costPerBaseUnit()).isEqualByComparingTo("3.044444");
        assertThat(oil.referencePrice()).isEqualByComparingTo("3044.44");
        assertThat(oil.referencePriceUnit()).isEqualTo(ReferencePriceUnit.LITER);
    }

    @Test
    void priceUpdateRecalculatesBaseAndReferenceCostsWithoutTouchingLegacyStock() {
        IngredientResponseDTO created = service.createIngredient(request(
                "Carne", MeasurementUnit.GRAM, "Bolsa", "1000", "9999"));
        Ingredient entity = repository.findById(created.id()).orElseThrow();
        entity.setCurrentStock(new BigDecimal("77.0000"));
        entity.setMinimumStock(new BigDecimal("11.0000"));
        repository.saveAndFlush(entity);

        IngredientResponseDTO updated = service.updateCost(created.id(),
                new IngredientCostUpdateDTO("Bolsa", new BigDecimal("1000"),
                        new BigDecimal("10500")));

        assertThat(updated.costPerBaseUnit()).isEqualByComparingTo("10.5");
        assertThat(updated.referencePrice()).isEqualByComparingTo("10500.00");
        Ingredient stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.getCurrentStock()).isEqualByComparingTo("77");
        assertThat(stored.getMinimumStock()).isEqualByComparingTo("11");
    }

    @Test
    void legacyStockFieldsAreAcceptedButIgnoredAndNeverExposed() throws Exception {
        String response = mockMvc.perform(post(BASE).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Queso crema",
                                  "measurementUnit":"GRAM",
                                  "purchasePresentation":"Bolsa",
                                  "purchaseQuantity":3000,
                                  "purchasePrice":23436,
                                  "currentStock":5000,
                                  "minimumStock":1000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costPerBaseUnit").value(7.812))
                .andExpect(jsonPath("$.referencePrice").value(7812.00))
                .andExpect(jsonPath("$.referencePriceUnit").value("KG"))
                .andExpect(jsonPath("$.currentStock").doesNotExist())
                .andExpect(jsonPath("$.minimumStock").doesNotExist())
                .andExpect(jsonPath("$.stockValue").doesNotExist())
                .andExpect(jsonPath("$.lowStock").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("Queso crema");
        Ingredient stored = repository.findByNameIgnoreCase("Queso crema").orElseThrow();
        assertThat(stored.getCurrentStock()).isZero();
        assertThat(stored.getMinimumStock()).isZero();
    }

    @Test
    void deprecatedStockEndpointsReturnGoneAndDoNotModifyLegacyColumns() throws Exception {
        IngredientResponseDTO created = service.createIngredient(request(
                "Cebolla", MeasurementUnit.GRAM, "Bolsa", "1000", "1500"));

        mockMvc.perform(patch(BASE + "/" + created.id() + "/stock").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentStock\":999}"))
                .andExpect(status().isGone());
        mockMvc.perform(patch(BASE + "/" + created.id() + "/stock/increase").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10}"))
                .andExpect(status().isGone());
        mockMvc.perform(patch(BASE + "/" + created.id() + "/minimum-stock").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minimumStock\":10}"))
                .andExpect(status().isGone());
        mockMvc.perform(get(BASE + "/low-stock").with(jwt()))
                .andExpect(status().isGone());

        Ingredient stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.getCurrentStock()).isZero();
        assertThat(stored.getMinimumStock()).isZero();
    }

    @Test
    void summaryContainsOnlyCatalogCounters() throws Exception {
        IngredientResponseDTO active = service.createIngredient(request(
                "Carne", MeasurementUnit.GRAM, "Bolsa", "1000", "9999"));
        IngredientResponseDTO inactive = service.createIngredient(request(
                "Cebolla", MeasurementUnit.GRAM, "Bolsa", "1000", "1500"));
        service.deactivateIngredient(inactive.id());

        IngredientSummaryDTO summary = service.getSummary();
        assertThat(summary.totalIngredients()).isEqualTo(2);
        assertThat(summary.activeIngredients()).isEqualTo(1);
        assertThat(summary.inactiveIngredients()).isEqualTo(1);
        assertThat(service.getIngredients(PageRequest.of(0, 10)).getContent())
                .extracting(IngredientResponseDTO::id).containsExactly(active.id());

        mockMvc.perform(get(BASE + "/summary").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngredients").value(2))
                .andExpect(jsonPath("$.lowStockIngredients").doesNotExist())
                .andExpect(jsonPath("$.totalStockValue").doesNotExist());
    }

    @Test
    void validatesPurchaseDataAndDuplicateNames() {
        service.createIngredient(request(
                "Carne", MeasurementUnit.GRAM, "Bolsa", "1000", "9999"));

        assertThatThrownBy(() -> service.createIngredient(request(
                " carne ", MeasurementUnit.GRAM, "Bolsa", "1000", "9999")))
                .isInstanceOf(IngredientAlreadyExistsException.class);
        assertThatThrownBy(() -> service.createIngredient(new IngredientRequestDTO(
                "Aceite", MeasurementUnit.MILLILITER, "Botella", BigDecimal.ZERO,
                new BigDecimal("2740"))))
                .isInstanceOf(InvalidIngredientException.class)
                .hasMessageContaining("mayor a cero");
    }

    private IngredientRequestDTO request(String name, MeasurementUnit unit,
            String presentation, String quantity, String price) {
        return new IngredientRequestDTO(name, unit, presentation,
                new BigDecimal(quantity), new BigDecimal(price));
    }
}
