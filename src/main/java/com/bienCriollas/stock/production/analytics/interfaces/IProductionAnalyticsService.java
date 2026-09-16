package com.bienCriollas.stock.production.analytics.interfaces;

import java.time.LocalDate;
import java.util.List;

import com.bienCriollas.stock.production.analytics.dto.*;

public interface IProductionAnalyticsService {

    CostPerformanceSummaryDTO getSummary(LocalDate from, LocalDate to);

    LaborPerformanceSummaryDTO getLaborPerformance(LocalDate from, LocalDate to);

    ProductionCostDetailDTO getProductionDetail(Long productionId);

    List<VarietyPerformanceDTO> getVarietyPerformance(LocalDate from, LocalDate to);

    VarietyPerformanceDTO getVarietyPerformance(Long varietyId, LocalDate from, LocalDate to);

    List<IngredientDeviationDTO> getIngredientDeviations(LocalDate from, LocalDate to);

    List<WasteReasonSummaryDTO> getWasteSummary(LocalDate from, LocalDate to);

    List<ProductionRankingDTO> getBestProductions(LocalDate from, LocalDate to, int limit);

    List<ProductionRankingDTO> getWorstProductions(LocalDate from, LocalDate to, int limit);

    ProductionCostSettingsResponseDTO getSettings();

    ProductionCostSettingsResponseDTO updateSettings(ProductionCostSettingsRequestDTO dto);
}
