package com.bienCriollas.stock.production.analytics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.production.analytics.entity.ProductionCostSettings;

public interface ProductionCostSettingsRepository
        extends JpaRepository<ProductionCostSettings, Long> {

    Optional<ProductionCostSettings> findFirstByOrderByIdAsc();
}
