package com.bienCriollas.stock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.bienCriollas.stock.expense.dto.ExpenseTypeDTO;
import com.bienCriollas.stock.expense.entity.Expense;
import com.bienCriollas.stock.expense.enums.ExpenseType;
import com.bienCriollas.stock.stock.dto.StockDTO;
import com.bienCriollas.stock.variety.dto.UpdatePriceDTO;
import com.bienCriollas.stock.variety.entity.EmpanadaVariety;
import com.bienCriollas.stock.waste.dto.EmpanadaLossDTO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ExternalJsonContractTest {

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Test
    void acceptsExistingFrontendRequestFieldNames() throws Exception {
        StockDTO stock = jsonMapper.readValue("""
                {
                  "id_variedad": 2,
                  "fecha_elaboracion": "2026-08-26",
                  "stock_total": 48
                }
                """, StockDTO.class);
        UpdatePriceDTO prices = jsonMapper.readValue("""
                {
                  "precioUnitario": 1800,
                  "precioMediaDocena": 9500,
                  "precioDocena": 18000
                }
                """, UpdatePriceDTO.class);
        ExpenseTypeDTO expense = jsonMapper.readValue("""
                {
                  "idCaja": 1,
                  "tipoEgreso": "PRODUCCION",
                  "descripcion": "Compra de harina",
                  "monto": 25000
                }
                """, ExpenseTypeDTO.class);
        EmpanadaLossDTO loss = jsonMapper.readValue("""
                {"idVariedad": 2, "cantidad": 3}
                """, EmpanadaLossDTO.class);

        assertEquals(2L, stock.varietyId());
        assertEquals(LocalDate.of(2026, 8, 26), stock.productionDate());
        assertEquals(48, stock.totalStock());
        assertEquals(new BigDecimal("1800"), prices.unitPrice());
        assertEquals(ExpenseType.PRODUCCION, expense.expenseType());
        assertEquals(3, loss.quantity());
    }

    @Test
    void keepsExistingFrontendResponseFieldNames() throws Exception {
        EmpanadaVariety variety = EmpanadaVariety.builder()
                .varietyId(2L)
                .name("Pollo")
                .unitPrice(new BigDecimal("1800"))
                .halfDozenPrice(new BigDecimal("9500"))
                .dozenPrice(new BigDecimal("18000"))
                .active(1)
                .build();
        Expense expense = Expense.builder()
                .expenseId(7L)
                .expenseType(ExpenseType.PRODUCCION)
                .description("Compra de harina")
                .amount(new BigDecimal("25000"))
                .build();

        JsonNode varietyJson = jsonMapper.valueToTree(variety);
        JsonNode expenseJson = jsonMapper.valueToTree(expense);

        assertTrue(varietyJson.has("id_variedad"));
        assertTrue(varietyJson.has("nombre"));
        assertTrue(varietyJson.has("precioUnitario"));
        assertTrue(varietyJson.has("activo"));
        assertFalse(varietyJson.has("varietyId"));
        assertFalse(varietyJson.has("unitPrice"));

        assertTrue(expenseJson.has("idEgreso"));
        assertTrue(expenseJson.has("tipoEgreso"));
        assertTrue(expenseJson.has("descripcion"));
        assertTrue(expenseJson.has("monto"));
        assertFalse(expenseJson.has("expenseId"));
        assertFalse(expenseJson.has("expenseType"));
    }
}
