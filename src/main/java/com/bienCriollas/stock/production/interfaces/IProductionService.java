package com.bienCriollas.stock.production.interfaces;

import java.time.LocalDate;
import org.springframework.data.domain.*;
import com.bienCriollas.stock.production.dto.*;
import com.bienCriollas.stock.production.enums.ProductionStatus;

public interface IProductionService {
    ProductionResponseDTO createProduction(ProductionCreateRequestDTO dto);
    ProductionResponseDTO getProductionById(Long id);
    Page<ProductionResponseDTO> getProductions(Pageable pageable);
    Page<ProductionResponseDTO> getProductionsByStatus(ProductionStatus status, Pageable pageable);
    Page<ProductionResponseDTO> getProductionsByDateRange(LocalDate from, LocalDate to, Pageable pageable);
    ProductionResponseDTO updateProduction(Long id, ProductionUpdateDTO dto);
    ProductionResponseDTO updateIngredientConsumption(Long productionId, ProductionIngredientUpdateDTO dto);
    ProductionResponseDTO addExtraIngredient(Long productionId, ProductionIngredientUpdateDTO dto);
    ProductionResponseDTO finalizeProduction(Long id);
    ProductionResponseDTO cancelProduction(Long id);
}
