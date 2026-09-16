package com.bienCriollas.stock.production.ingredient.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.bienCriollas.stock.production.ingredient.entity.Ingredient;
import com.bienCriollas.stock.production.ingredient.dto.IngredientSummaryDTO;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Ingredient> findByNameIgnoreCase(String name);

    Page<Ingredient> findByActiveTrue(Pageable pageable);

    Page<Ingredient> findByActiveFalse(Pageable pageable);

    // Spring Data escapa los comodines del usuario para buscar texto literal.
    @Query("""
            SELECT i FROM Ingredient i
            WHERE i.active = true
            AND LOWER(i.name) LIKE LOWER(CONCAT('%', :#{escape(#query)}, '%')) ESCAPE :#{escapeCharacter()}
            """)
    Page<Ingredient> search(@Param("query") String query, Pageable pageable);

    @Query("""
            SELECT i FROM Ingredient i
            WHERE i.active = true AND i.currentStockGrams <= i.minimumStockGrams
            ORDER BY i.name, i.id
            """)
    List<Ingredient> findLowStockIngredients();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Ingredient i WHERE i.id = :id")
    Optional<Ingredient> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT i FROM Ingredient i WHERE i.id IN :ids ORDER BY i.id")
    List<Ingredient> findAllByIdForRecipe(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Ingredient i WHERE i.id IN :ids ORDER BY i.id")
    List<Ingredient> findAllByIdForProduction(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT new com.bienCriollas.stock.production.ingredient.dto.IngredientSummaryDTO(
                COUNT(i),
                COALESCE(SUM(CASE WHEN i.active = true THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN i.active = false THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN i.active = true AND i.currentStockGrams <= i.minimumStockGrams THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(i.currentStockGrams * i.costPerKilogram * 0.001BD), 0BD))
            FROM Ingredient i
            """)
    IngredientSummaryDTO getSummary();
}
