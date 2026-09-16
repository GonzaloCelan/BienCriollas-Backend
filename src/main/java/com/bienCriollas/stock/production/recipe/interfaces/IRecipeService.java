package com.bienCriollas.stock.production.recipe.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.production.recipe.dto.RecipeCalculationResponseDTO;
import com.bienCriollas.stock.production.recipe.dto.RecipeRequestDTO;
import com.bienCriollas.stock.production.recipe.dto.RecipeResponseDTO;
import com.bienCriollas.stock.production.recipe.dto.RecipeVersionRequestDTO;

public interface IRecipeService {

    RecipeResponseDTO createRecipe(RecipeRequestDTO dto);
    RecipeResponseDTO getRecipeById(Long id);
    RecipeResponseDTO getActiveRecipeByVariety(Long varietyId);
    Page<RecipeResponseDTO> getActiveRecipes(Pageable pageable);
    Page<RecipeResponseDTO> getRecipesByStatus(Boolean active, Pageable pageable);
    List<RecipeResponseDTO> getRecipeHistory(Long varietyId);
    RecipeResponseDTO createNewVersion(Long recipeId, RecipeVersionRequestDTO dto);
    RecipeCalculationResponseDTO calculateRecipe(Long recipeId, Integer quantity);
}

