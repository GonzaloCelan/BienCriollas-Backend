package com.bienCriollas.stock.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.repository.StockRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:stock-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.datasource.hikari.maximum-pool-size=20",
                "springdoc.api-docs.enabled=false"
        })
class StockServiceConcurrencyTest {

    private static final int PEDIDOS_SIMULTANEOS = 40;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    private ExecutorService executor;

    @BeforeEach
    void prepararStock() {
        stockRepository.deleteAll();
        stockRepository.saveAndFlush(Stock.builder()
                .idVariedad(1L)
                .fechaElaboracion(LocalDate.now())
                .stockTotal(100)
                .stockDisponible(100)
                .activo(1)
                .build());
        stockRepository.saveAndFlush(Stock.builder()
                .idVariedad(2L)
                .fechaElaboracion(LocalDate.now())
                .stockTotal(100)
                .stockDisponible(100)
                .activo(1)
                .build());
        executor = Executors.newFixedThreadPool(PEDIDOS_SIMULTANEOS);
    }

    @AfterEach
    void cerrarExecutor() {
        executor.shutdownNow();
    }

    @Test
    void cuarentaDescuentosSimultaneosDescuentanCuarentaUnidades() throws Exception {
        CountDownLatch listos = new CountDownLatch(PEDIDOS_SIMULTANEOS);
        CountDownLatch comenzar = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();

        for (int i = 0; i < PEDIDOS_SIMULTANEOS; i++) {
            resultados.add(executor.submit(() -> {
                listos.countDown();
                comenzar.await();
                return stockService.descontarStockVariedad(1L, 1);
            }));
        }

        assertTrue(listos.await(10, TimeUnit.SECONDS));
        comenzar.countDown();

        for (Future<Boolean> resultado : resultados) {
            assertTrue(resultado.get(30, TimeUnit.SECONDS));
        }

        Stock stockFinal = stockRepository.findByIdVariedadAndActivo(1L, 1).orElseThrow();
        assertEquals(60, stockFinal.getStockDisponible());
    }

    @Test
    void cuarentaEdicionesSimultaneasActualizanDosVariedadesSinDeadlocks() throws Exception {
        CountDownLatch listos = new CountDownLatch(PEDIDOS_SIMULTANEOS);
        CountDownLatch comenzar = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();

        for (int i = 0; i < PEDIDOS_SIMULTANEOS; i++) {
            resultados.add(executor.submit(() -> {
                listos.countDown();
                comenzar.await();
                stockService.ajustarDisponibilidad(Map.of(1L, 1, 2L, -1));
                return true;
            }));
        }

        assertTrue(listos.await(10, TimeUnit.SECONDS));
        comenzar.countDown();

        for (Future<Boolean> resultado : resultados) {
            assertTrue(resultado.get(30, TimeUnit.SECONDS));
        }

        Stock stockDevuelto = stockRepository.findByIdVariedadAndActivo(1L, 1).orElseThrow();
        Stock stockDescontado = stockRepository.findByIdVariedadAndActivo(2L, 1).orElseThrow();
        assertEquals(140, stockDevuelto.getStockDisponible());
        assertEquals(60, stockDescontado.getStockDisponible());
    }
}
