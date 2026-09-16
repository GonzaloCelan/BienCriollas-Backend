package com.bienCriollas.stock.production.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.production.analytics.dto.*;
import com.bienCriollas.stock.production.analytics.entity.ProductionCostSettings;
import com.bienCriollas.stock.production.analytics.exception.*;
import com.bienCriollas.stock.production.analytics.interfaces.IProductionAnalyticsService;
import com.bienCriollas.stock.production.analytics.repository.ProductionCostSettingsRepository;
import com.bienCriollas.stock.production.entity.*;
import com.bienCriollas.stock.production.enums.ProductionStatus;
import com.bienCriollas.stock.production.repository.ProductionRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionAnalyticsService implements IProductionAnalyticsService {

    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ProductionRepository productionRepository;
    private final ProductionCostSettingsRepository settingsRepository;
    private final ProductionCostCalculator calculator;
    private final Validator validator;

    @Override
    public CostPerformanceSummaryDTO getSummary(LocalDate from, LocalDate to) {
        List<ProductionCostDetailDTO> details = loadDetails(from, to);
        int totalUnits = details.stream().map(ProductionCostDetailDTO::finalUnits)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int totalWaste = details.stream().map(ProductionCostDetailDTO::wasteUnits)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        BigDecimal totalIngredient = sum(details, ProductionCostDetailDTO::actualIngredientCost);
        BigDecimal totalLabor = sum(details, ProductionCostDetailDTO::actualLaborCost);
        BigDecimal totalEnergy = sum(details, ProductionCostDetailDTO::energyCost);
        BigDecimal totalProduction = sum(details, ProductionCostDetailDTO::actualTotalCost);
        BigDecimal totalPersonHours = sum(details, ProductionCostDetailDTO::actualPersonHours);
        BigDecimal averageUnitsPerPersonHour = weightedUnitsPerPersonHour(details);
        BigDecimal averageVariation = average(
                details, ProductionCostDetailDTO::laborProductivityVariationPercentage);
        BigDecimal laborInefficiency = sumPositive(
                details, ProductionCostDetailDTO::laborInefficiencyCost);
        BigDecimal wasteCost = sum(details, ProductionCostDetailDTO::estimatedWasteCost);
        long efficient = details.stream()
                .map(ProductionCostDetailDTO::laborProductivityVariationPercentage)
                .filter(Objects::nonNull).filter(value -> value.signum() >= 0).count();
        long inefficient = details.stream()
                .map(ProductionCostDetailDTO::laborProductivityVariationPercentage)
                .filter(Objects::nonNull).filter(value -> value.signum() < 0).count();

        return new CostPerformanceSummaryDTO(
                from, to, (long) details.size(), totalUnits, totalWaste,
                money(totalIngredient), money(totalLabor), money(totalEnergy),
                money(totalProduction), money(perUnit(totalProduction, totalUnits)),
                metric(totalPersonHours), metric(averageUnitsPerPersonHour),
                metric(averageVariation), money(laborInefficiency), money(wasteCost),
                efficient, inefficient);
    }

    @Override
    public LaborPerformanceSummaryDTO getLaborPerformance(LocalDate from, LocalDate to) {
        List<ProductionCostDetailDTO> details = loadDetails(from, to);
        BigDecimal actualHours = sum(details, ProductionCostDetailDTO::actualPersonHours);
        BigDecimal expectedHours = sum(details, ProductionCostDetailDTO::expectedPersonHoursForActualOutput);
        BigDecimal actualProductivity = weightedUnitsPerPersonHour(details);
        BigDecimal standardProductivity = weightedStandardUnitsPerPersonHour(details);
        BigDecimal actualLaborCost = sum(details, ProductionCostDetailDTO::actualLaborCost);
        BigDecimal expectedLaborCost = sum(
                details, ProductionCostDetailDTO::expectedLaborCostForActualOutput);
        BigDecimal inefficiencyCost = sumPositive(
                details, ProductionCostDetailDTO::laborInefficiencyCost);

        return new LaborPerformanceSummaryDTO(
                metric(actualHours),
                metric(expectedHours),
                metric(subtract(actualHours, expectedHours)),
                metric(actualProductivity),
                metric(standardProductivity),
                metric(variation(actualProductivity, standardProductivity)),
                money(actualLaborCost),
                money(expectedLaborCost),
                money(inefficiencyCost));
    }

    @Override
    public ProductionCostDetailDTO getProductionDetail(Long productionId) {
        if (productionId == null || productionId <= 0) {
            throw new InvalidProductionAnalyticsException("El id de producción debe ser mayor a cero.");
        }
        Production production = productionRepository.findDetailedById(productionId)
                .filter(item -> item.getStatus() == ProductionStatus.FINALIZED)
                .orElseThrow(() -> new ProductionAnalyticsNotFoundException(productionId));
        return calculator.calculate(production);
    }

    @Override
    public List<VarietyPerformanceDTO> getVarietyPerformance(LocalDate from, LocalDate to) {
        Map<Long, List<ProductionCostDetailDTO>> byVariety = loadDetails(from, to).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProductionCostDetailDTO::varietyId, TreeMap::new,
                        java.util.stream.Collectors.toList()));
        return byVariety.values().stream().map(this::toVarietyPerformance)
                .sorted(Comparator.comparing(VarietyPerformanceDTO::varietyName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public VarietyPerformanceDTO getVarietyPerformance(
            Long varietyId, LocalDate from, LocalDate to) {
        if (varietyId == null || varietyId <= 0) {
            throw new InvalidProductionAnalyticsException("El id de variedad debe ser mayor a cero.");
        }
        List<ProductionCostDetailDTO> details = loadDetails(from, to).stream()
                .filter(item -> item.varietyId().equals(varietyId)).toList();
        if (details.isEmpty()) {
            throw new ProductionAnalyticsNotFoundException(
                    "No hay producciones finalizadas para la variedad " + varietyId
                            + " en el período indicado.");
        }
        return toVarietyPerformance(details);
    }

    @Override
    public List<IngredientDeviationDTO> getIngredientDeviations(LocalDate from, LocalDate to) {
        List<Production> productions = loadProductions(from, to);
        Map<Long, IngredientAccumulator> totals = new HashMap<>();
        for (Production production : productions) {
            for (ProductionIngredient item : production.getIngredients()) {
                IngredientAccumulator total = totals.computeIfAbsent(
                        item.getIngredient().getId(), ignored -> new IngredientAccumulator(
                                item.getIngredient().getId(), item.getIngredient().getName()));
                BigDecimal actual = item.getActualQuantityGrams() == null
                        ? item.getExpectedQuantityGrams() : item.getActualQuantityGrams();
                total.expected = total.expected.add(item.getExpectedQuantityGrams());
                total.actual = total.actual.add(actual);
                total.costDeviation = total.costDeviation.add(
                        actual.subtract(item.getExpectedQuantityGrams())
                                .multiply(item.getCostPerGramSnapshot()));
            }
        }
        return totals.values().stream().map(total -> {
            BigDecimal difference = total.actual.subtract(total.expected);
            BigDecimal percentage = total.expected.signum() == 0 ? null
                    : difference.multiply(ONE_HUNDRED).divide(
                            total.expected, 10, RoundingMode.HALF_UP);
            return new IngredientDeviationDTO(
                    total.id, total.name, grams(total.expected), grams(total.actual),
                    grams(difference), metric(percentage), money(total.costDeviation));
        }).sorted(Comparator.comparing(
                        IngredientDeviationDTO::additionalCost,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(IngredientDeviationDTO::ingredientName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<WasteReasonSummaryDTO> getWasteSummary(LocalDate from, LocalDate to) {
        List<Production> productions = loadProductions(from, to);
        List<ProductionCostDetailDTO> details = productions.stream()
                .map(calculator::calculate)
                .filter(item -> item.wasteUnits() != null && item.wasteUnits() > 0).toList();
        int totalWaste = details.stream().mapToInt(ProductionCostDetailDTO::wasteUnits).sum();
        Map<String, WasteAccumulator> totals = new HashMap<>();
        Map<Long, String> reasons = productions.stream().collect(
                java.util.stream.Collectors.toMap(
                        Production::getId,
                        production -> normalizeReason(production.getWasteReason())));
        for (ProductionCostDetailDTO detail : details) {
            String reason = reasons.getOrDefault(detail.productionId(), "Sin motivo informado");
            WasteAccumulator total = totals.computeIfAbsent(reason, ignored -> new WasteAccumulator());
            total.units += detail.wasteUnits();
            if (detail.estimatedWasteCost() != null) {
                total.cost = total.cost.add(detail.estimatedWasteCost());
            } else {
                total.costAvailable = false;
            }
        }
        return totals.entrySet().stream().map(entry -> new WasteReasonSummaryDTO(
                        entry.getKey(), entry.getValue().units,
                        totalWaste == 0 ? BigDecimal.ZERO.setScale(2)
                                : metric(BigDecimal.valueOf(entry.getValue().units)
                                        .multiply(ONE_HUNDRED).divide(
                                                BigDecimal.valueOf(totalWaste), 10,
                                                RoundingMode.HALF_UP)),
                        entry.getValue().costAvailable ? money(entry.getValue().cost) : null))
                .sorted(Comparator.comparing(
                                WasteReasonSummaryDTO::estimatedCost,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WasteReasonSummaryDTO::reason,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<ProductionRankingDTO> getBestProductions(
            LocalDate from, LocalDate to, int limit) {
        validateLimit(limit);
        return loadDetails(from, to).stream()
                .filter(item -> item.laborProductivityVariationPercentage() != null)
                .sorted(Comparator.comparing(
                                ProductionCostDetailDTO::laborProductivityVariationPercentage)
                        .reversed().thenComparing(ProductionCostDetailDTO::productionId))
                .limit(limit).map(this::toRanking).toList();
    }

    @Override
    public List<ProductionRankingDTO> getWorstProductions(
            LocalDate from, LocalDate to, int limit) {
        validateLimit(limit);
        return loadDetails(from, to).stream()
                .filter(item -> item.laborInefficiencyCost() != null)
                .sorted(Comparator.comparing(
                                ProductionCostDetailDTO::laborInefficiencyCost)
                        .reversed().thenComparing(
                                ProductionCostDetailDTO::laborProductivityVariationPercentage,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit).map(this::toRanking).toList();
    }

    @Override
    public ProductionCostSettingsResponseDTO getSettings() {
        return toSettingsResponse(requireSettings());
    }

    @Override
    @Transactional
    public ProductionCostSettingsResponseDTO updateSettings(
            ProductionCostSettingsRequestDTO dto) {
        if (dto == null) {
            throw new InvalidProductionAnalyticsException(
                    "La configuración de costos es obligatoria.");
        }
        validator.validate(dto).stream().findFirst().ifPresent(violation -> {
            throw new InvalidProductionAnalyticsException(
                    violation.getPropertyPath() + ": " + violation.getMessage());
        });
        ProductionCostSettings settings = requireSettings();
        settings.setAverageHourlyLaborCost(dto.averageHourlyLaborCost().setScale(2));
        settings.setEnergyPercentage(dto.energyPercentage().setScale(2));
        return toSettingsResponse(settingsRepository.saveAndFlush(settings));
    }

    private List<ProductionCostDetailDTO> loadDetails(LocalDate from, LocalDate to) {
        return loadProductions(from, to).stream().map(calculator::calculate).toList();
    }

    private List<Production> loadProductions(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return productionRepository.findForAnalytics(
                ProductionStatus.FINALIZED, from, to);
    }

    private VarietyPerformanceDTO toVarietyPerformance(
            List<ProductionCostDetailDTO> details) {
        ProductionCostDetailDTO first = details.get(0);
        int units = details.stream().map(ProductionCostDetailDTO::finalUnits)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int waste = details.stream().map(ProductionCostDetailDTO::wasteUnits)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int planned = details.stream().map(ProductionCostDetailDTO::plannedUnits)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        BigDecimal ingredient = sum(details, ProductionCostDetailDTO::actualIngredientCost);
        BigDecimal labor = sum(details, ProductionCostDetailDTO::actualLaborCost);
        BigDecimal total = sum(details, ProductionCostDetailDTO::actualTotalCost);
        return new VarietyPerformanceDTO(
                first.varietyId(), first.varietyName(), (long) details.size(), units, waste,
                planned == 0 ? null : metric(BigDecimal.valueOf(waste)
                        .multiply(ONE_HUNDRED).divide(
                                BigDecimal.valueOf(planned), 10, RoundingMode.HALF_UP)),
                money(ingredient), money(labor), money(total), money(perUnit(total, units)),
                metric(weightedUnitsPerHour(details)),
                metric(weightedUnitsPerPersonHour(details)),
                metric(average(details,
                        ProductionCostDetailDTO::laborProductivityVariationPercentage)),
                money(sumPositive(details, ProductionCostDetailDTO::laborInefficiencyCost)),
                money(sum(details, ProductionCostDetailDTO::totalCostDeviation)));
    }

    private BigDecimal weightedUnitsPerHour(List<ProductionCostDetailDTO> details) {
        BigDecimal hours = BigDecimal.ZERO;
        int units = 0;
        for (ProductionCostDetailDTO detail : details) {
            if (detail.totalMinutes() != null && detail.totalMinutes() > 0
                    && detail.finalUnits() != null) {
                hours = hours.add(BigDecimal.valueOf(detail.totalMinutes())
                        .divide(SIXTY, 10, RoundingMode.HALF_UP));
                units += detail.finalUnits();
            }
        }
        return hours.signum() == 0 ? null
                : BigDecimal.valueOf(units).divide(hours, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedUnitsPerPersonHour(List<ProductionCostDetailDTO> details) {
        BigDecimal hours = BigDecimal.ZERO;
        int units = 0;
        for (ProductionCostDetailDTO detail : details) {
            if (detail.actualPersonHours() != null && detail.finalUnits() != null) {
                hours = hours.add(detail.actualPersonHours());
                units += detail.finalUnits();
            }
        }
        return hours.signum() == 0 ? null
                : BigDecimal.valueOf(units).divide(hours, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedStandardUnitsPerPersonHour(
            List<ProductionCostDetailDTO> details) {
        BigDecimal hours = BigDecimal.ZERO;
        int units = 0;
        for (ProductionCostDetailDTO detail : details) {
            if (detail.expectedPersonHoursForActualOutput() != null
                    && detail.finalUnits() != null) {
                hours = hours.add(detail.expectedPersonHoursForActualOutput());
                units += detail.finalUnits();
            }
        }
        return hours.signum() == 0 ? null
                : BigDecimal.valueOf(units).divide(hours, 10, RoundingMode.HALF_UP);
    }

    private ProductionRankingDTO toRanking(ProductionCostDetailDTO detail) {
        return new ProductionRankingDTO(
                detail.productionId(), detail.productionDate(), detail.varietyName(),
                detail.finalUnits(), detail.actualUnitsPerPersonHour(),
                detail.laborProductivityVariationPercentage(),
                detail.laborInefficiencyCost(), detail.actualCostPerUnit());
    }

    private ProductionCostSettings requireSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ProductionAnalyticsNotFoundException(
                        "No existe una configuración de costos de producción."));
    }

    private ProductionCostSettingsResponseDTO toSettingsResponse(
            ProductionCostSettings settings) {
        return new ProductionCostSettingsResponseDTO(
                settings.getAverageHourlyLaborCost(),
                settings.getEnergyPercentage(), settings.getUpdatedAt());
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidProductionAnalyticsException(
                    "Las fechas desde y hasta son obligatorias.");
        }
        if (from.isAfter(to)) {
            throw new InvalidProductionAnalyticsException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new InvalidProductionAnalyticsException(
                    "El límite debe estar entre 1 y 100.");
        }
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank()
                ? "Sin motivo informado" : reason.strip();
    }

    private BigDecimal sum(
            List<ProductionCostDetailDTO> details,
            Function<ProductionCostDetailDTO, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (ProductionCostDetailDTO detail : details) {
            BigDecimal value = getter.apply(detail);
            if (value == null) return null;
            total = total.add(value);
        }
        return total;
    }

    private BigDecimal sumPositive(
            List<ProductionCostDetailDTO> details,
            Function<ProductionCostDetailDTO, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (ProductionCostDetailDTO detail : details) {
            BigDecimal value = getter.apply(detail);
            if (value == null) return null;
            if (value.signum() > 0) total = total.add(value);
        }
        return total;
    }

    private BigDecimal average(
            List<ProductionCostDetailDTO> details,
            Function<ProductionCostDetailDTO, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ProductionCostDetailDTO detail : details) {
            BigDecimal value = getter.apply(detail);
            if (value != null) {
                total = total.add(value);
                count++;
            }
        }
        return count == 0 ? null
                : total.divide(BigDecimal.valueOf(count), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal perUnit(BigDecimal total, int units) {
        return total == null || units <= 0 ? null
                : total.divide(BigDecimal.valueOf(units), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal variation(BigDecimal actual, BigDecimal standard) {
        return actual == null || standard == null || standard.signum() <= 0 ? null
                : actual.divide(standard, 10, RoundingMode.HALF_UP)
                        .subtract(BigDecimal.ONE).multiply(ONE_HUNDRED);
    }

    private BigDecimal subtract(BigDecimal first, BigDecimal second) {
        return first == null || second == null ? null : first.subtract(second);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal metric(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal grams(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class IngredientAccumulator {
        private final Long id;
        private final String name;
        private BigDecimal expected = BigDecimal.ZERO;
        private BigDecimal actual = BigDecimal.ZERO;
        private BigDecimal costDeviation = BigDecimal.ZERO;

        private IngredientAccumulator(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class WasteAccumulator {
        private int units;
        private BigDecimal cost = BigDecimal.ZERO;
        private boolean costAvailable = true;
    }
}
