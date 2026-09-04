package com.bienCriollas.stock.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bienCriollas.stock.waste.repository.WasteRepository;
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
}
