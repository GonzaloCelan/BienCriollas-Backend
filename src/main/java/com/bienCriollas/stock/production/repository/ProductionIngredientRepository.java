package com.bienCriollas.stock.production.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bienCriollas.stock.production.entity.ProductionIngredient;

public interface ProductionIngredientRepository extends JpaRepository<ProductionIngredient, Long> {
    List<ProductionIngredient> findByProductionIdOrderByIngredientId(Long productionId);
}
