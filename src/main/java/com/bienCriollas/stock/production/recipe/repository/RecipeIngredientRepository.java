package com.bienCriollas.stock.production.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bienCriollas.stock.production.recipe.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipeId(Long recipeId);
    boolean existsByRecipeIdAndIngredientId(Long recipeId, Long ingredientId);
    boolean existsByIngredientId(Long ingredientId);
}
