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

    private static final int CONCURRENT_ORDERS = 40;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    private ExecutorService executor;

    @BeforeEach
    void prepareStock() {
        stockRepository.deleteAll();
        stockRepository.saveAndFlush(Stock.builder()
                .varietyId(1L)
                .productionDate(LocalDate.now())
                .totalStock(100)
                .availableStock(100)
                .active(1)
                .build());
        stockRepository.saveAndFlush(Stock.builder()
                .varietyId(2L)
                .productionDate(LocalDate.now())
                .totalStock(100)
                .availableStock(100)
                .active(1)
                .build());
        executor = Executors.newFixedThreadPool(CONCURRENT_ORDERS);
    }

    @AfterEach
    void closeExecutor() {
        executor.shutdownNow();
    }

    @Test
    void fortyConcurrentDecreasesRemoveFortyUnits() throws Exception {
        CountDownLatch ready = new CountDownLatch(CONCURRENT_ORDERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_ORDERS; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return stockService.decreaseVarietyStock(1L, 1);
            }));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        for (Future<Boolean> result : results) {
            assertTrue(result.get(30, TimeUnit.SECONDS));
        }

        Stock finalStock = stockRepository.findByVarietyIdAndActive(1L, 1).orElseThrow();
        assertEquals(60, finalStock.getAvailableStock());
    }

    @Test
    void fortyConcurrentEditsUpdateTwoVarietiesWithoutDeadlocks() throws Exception {
        CountDownLatch ready = new CountDownLatch(CONCURRENT_ORDERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_ORDERS; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                stockService.adjustAvailability(Map.of(1L, 1, 2L, -1));
                return true;
            }));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        for (Future<Boolean> result : results) {
            assertTrue(result.get(30, TimeUnit.SECONDS));
        }

        Stock returnedStock = stockRepository.findByVarietyIdAndActive(1L, 1).orElseThrow();
        Stock decreasedStock = stockRepository.findByVarietyIdAndActive(2L, 1).orElseThrow();
        assertEquals(140, returnedStock.getAvailableStock());
        assertEquals(60, decreasedStock.getAvailableStock());
    }
}
