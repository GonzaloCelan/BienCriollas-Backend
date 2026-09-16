package com.bienCriollas.stock.production.ingredient.interfaces;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.bienCriollas.stock.production.ingredient.dto.*;

public interface IIngredientService {
    IngredientResponseDTO createIngredient(IngredientRequestDTO dto);

    IngredientResponseDTO getIngredientById(Long id);

    Page<IngredientResponseDTO> getIngredients(Pageable pageable);

    Page<IngredientResponseDTO> getIngredientsByStatus(Boolean active, Pageable pageable);

    Page<IngredientResponseDTO> searchIngredients(String query, Pageable pageable);

    IngredientResponseDTO updateIngredient(Long id, IngredientRequestDTO dto);

    IngredientResponseDTO setStock(Long id, IngredientStockUpdateDTO dto);

    IngredientResponseDTO increaseStock(Long id, IngredientStockMovementDTO dto);

    IngredientResponseDTO decreaseStock(Long id, IngredientStockMovementDTO dto);

    IngredientResponseDTO updateCost(Long id, IngredientCostUpdateDTO dto);

    IngredientResponseDTO updateMinimumStock(Long id, IngredientMinimumStockDTO dto);

    IngredientResponseDTO activateIngredient(Long id);

    IngredientResponseDTO deactivateIngredient(Long id);

    List<IngredientResponseDTO> getLowStockIngredients();

    IngredientSummaryDTO getSummary();
}

