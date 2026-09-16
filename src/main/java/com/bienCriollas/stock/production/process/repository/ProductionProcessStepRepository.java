package com.bienCriollas.stock.production.process.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.production.process.entity.ProductionProcessStep;

public interface ProductionProcessStepRepository
        extends JpaRepository<ProductionProcessStep, Long> {

    List<ProductionProcessStep> findByProcessIdOrderByStepOrderAsc(Long processId);
}
