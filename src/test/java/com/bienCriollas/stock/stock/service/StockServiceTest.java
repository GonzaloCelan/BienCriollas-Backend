package com.bienCriollas.stock.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bienCriollas.stock.waste.repository.WasteRepository;
import com.bienCriollas.stock.production.recipe.interfaces.IRecipeService;
import com.bienCriollas.stock.stock.dto.StockActualResponseDTO;
import com.bienCriollas.stock.stock.dto.StockSummaryResponseDTO;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.exception.InsufficientStockException;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variety.repository.EmpanadaVarietyRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private EmpanadaVarietyRepository empanadaVarietyRepository;

    @Mock
    private WasteRepository wasteRepository;

    @Mock
    private IRecipeService recipeService;

    @InjectMocks
    private StockService stockService;

    @Test
    void adjustsSeveralVarietiesUnderTheSameLock() {
        Stock beef = Stock.builder().varietyId(1L).availableStock(100).active(1).build();
        Stock chicken = Stock.builder().varietyId(2L).availableStock(20).active(1).build();
        when(stockRepository.findActiveForUpdate(anyCollection()))
                .thenReturn(List.of(beef, chicken));

        stockService.adjustAvailability(Map.of(1L, -40, 2L, 5));

        assertEquals(60, beef.getAvailableStock());
        assertEquals(25, chicken.getAvailableStock());
        verify(stockRepository).saveAll(List.of(beef, chicken));
    }

    @Test
    void rejectsDecreaseWhenStockIsInsufficient() {
        Stock beef = Stock.builder().varietyId(1L).availableStock(9).active(1).build();
        when(stockRepository.findActiveForUpdate(anyCollection()))
                .thenReturn(List.of(beef));

        assertThrows(
                InsufficientStockException.class,
                () -> stockService.adjustAvailability(Map.of(1L, -10)));

        assertEquals(9, beef.getAvailableStock());
    }

    @Test
    void valuesCurrentStockWithOneBatchRecipeCostLookup() {
        Stock hamAndCheese = Stock.builder()
                .varietyId(1L)
                .productionDate(LocalDate.of(2026, 9, 29))
                .totalStock(165)
                .availableStock(100)
                .active(1)
                .build();
        Stock withoutRecipe = Stock.builder()
                .varietyId(2L)
                .productionDate(LocalDate.of(2026, 9, 29))
                .totalStock(50)
                .availableStock(40)
                .active(1)
                .build();
        when(stockRepository.findByActive(1)).thenReturn(List.of(hamAndCheese, withoutRecipe));
        when(recipeService.getActiveUnitCosts(anyCollection()))
                .thenReturn(Map.of(1L, new BigDecimal("618.67")));

        List<StockActualResponseDTO> result = stockService.getAllStockRecords();

        assertEquals(new BigDecimal("618.67"), result.get(0).currentUnitCost());
        assertEquals(new BigDecimal("61867.00"), result.get(0).currentStockValue());
        assertNull(result.get(1).currentUnitCost());
        assertNull(result.get(1).currentStockValue());
        verify(recipeService, times(1)).getActiveUnitCosts(anyCollection());
    }

    @Test
    void summarizesOnlyCalculatedValuesAndCountsMissingValuations() {
        Stock valued = Stock.builder().varietyId(1L).totalStock(100)
                .availableStock(100).active(1).build();
        Stock withoutRecipe = Stock.builder().varietyId(2L).totalStock(50)
                .availableStock(43).active(1).build();
        when(stockRepository.findByActive(1)).thenReturn(List.of(valued, withoutRecipe));
        when(recipeService.getActiveUnitCosts(anyCollection()))
                .thenReturn(Map.of(1L, new BigDecimal("618.67")));

        StockSummaryResponseDTO result = stockService.getStockSummary();

        assertEquals(143, result.totalAvailableUnits());
        assertEquals(new BigDecimal("61867.00"), result.totalStockValue());
        assertEquals(2, result.varietiesWithStock());
        assertEquals(1, result.varietiesWithoutValuation());
    }
}
