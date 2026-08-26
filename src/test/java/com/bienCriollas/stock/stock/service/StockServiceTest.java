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

import com.bienCriollas.stock.merma.repository.MermaRepository;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.exception.StockNoDisponibleException;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variedad.repository.VariedadEmpanadaRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private VariedadEmpanadaRepository variedadEmpanadaRepository;

    @Mock
    private MermaRepository mermaRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void ajustaVariasVariedadesBajoUnMismoBloqueo() {
        Stock carne = Stock.builder().idVariedad(1L).stockDisponible(100).activo(1).build();
        Stock pollo = Stock.builder().idVariedad(2L).stockDisponible(20).activo(1).build();
        when(stockRepository.findActivosParaActualizar(anyCollection()))
                .thenReturn(List.of(carne, pollo));

        stockService.ajustarDisponibilidad(Map.of(1L, -40, 2L, 5));

        assertEquals(60, carne.getStockDisponible());
        assertEquals(25, pollo.getStockDisponible());
        verify(stockRepository).saveAll(List.of(carne, pollo));
    }

    @Test
    void rechazaElDescuentoCuandoNoAlcanzaElStock() {
        Stock carne = Stock.builder().idVariedad(1L).stockDisponible(9).activo(1).build();
        when(stockRepository.findActivosParaActualizar(anyCollection()))
                .thenReturn(List.of(carne));

        assertThrows(
                StockNoDisponibleException.class,
                () -> stockService.ajustarDisponibilidad(Map.of(1L, -10)));

        assertEquals(9, carne.getStockDisponible());
    }
}
