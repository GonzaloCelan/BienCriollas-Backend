package com.bienCriollas.stock.production.recipe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.bienCriollas.stock.production.recipe.entity.Recipe;

import jakarta.persistence.LockModeType;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @EntityGraph(attributePaths = {"variety", "ingredients", "ingredients.ingredient"})
    @Query("SELECT DISTINCT r FROM Recipe r WHERE r.id = :id")
    Optional<Recipe> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"variety", "ingredients", "ingredients.ingredient"})
    Optional<Recipe> findByVarietyVarietyIdAndActiveTrue(Long varietyId);

    boolean existsByVarietyVarietyIdAndActiveTrue(Long varietyId);

    @EntityGraph(attributePaths = "variety")
    Page<Recipe> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "variety")
    Page<Recipe> findByActiveFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"variety", "ingredients", "ingredients.ingredient"})
    List<Recipe> findByVarietyVarietyIdOrderByVersionDesc(Long varietyId);

    Optional<Recipe> findTopByVarietyVarietyIdOrderByVersionDesc(Long varietyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recipe r WHERE r.variety.varietyId = :varietyId AND r.active = true")
    Optional<Recipe> findActiveByVarietyForUpdate(@Param("varietyId") Long varietyId);
}
