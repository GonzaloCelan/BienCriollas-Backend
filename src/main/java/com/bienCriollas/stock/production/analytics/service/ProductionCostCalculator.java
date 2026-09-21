package com.bienCriollas.stock.production.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.production.analytics.dto.ProductionCostDetailDTO;
import com.bienCriollas.stock.production.entity.*;
import com.bienCriollas.stock.production.process.entity.ProductionProcess;
import com.bienCriollas.stock.production.recipe.enums.*;

@Component
public class ProductionCostCalculator {
    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CALCULATION_SCALE = 10;

    public ProductionCostDetailDTO calculate(Production production) {
        BigDecimal expectedIngredientRaw = production.getIngredients().stream()
                .map(item -> item.getExpectedQuantity().multiply(item.getCostPerBaseUnitSnapshot()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualIngredientRaw = production.getIngredients().stream()
                .map(item -> actualQuantity(item).multiply(item.getCostPerBaseUnitSnapshot()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProductionProcess process = production.getProcess();
        BigDecimal standardUnitsPerHourRaw = standardUnitsPerHour(process);
        BigDecimal actualUnitsPerHourRaw = actualUnitsPerHour(production);
        BigDecimal productivityVariationRaw = variation(actualUnitsPerHourRaw, standardUnitsPerHourRaw);
        BigDecimal standardPersonHoursRaw = standardPersonHours(process);
        BigDecimal actualPersonHoursRaw = actualPersonHours(production);
        BigDecimal standardUnitsPerPersonHourRaw = unitsPerHour(
                process == null ? null : process.getReferenceYieldUnits(), standardPersonHoursRaw);
        BigDecimal actualUnitsPerPersonHourRaw = unitsPerHour(
                production.getFinalUnits(), actualPersonHoursRaw);
        BigDecimal laborVariationRaw = variation(
                actualUnitsPerPersonHourRaw, standardUnitsPerPersonHourRaw);

        BigDecimal expectedPersonHoursRaw = expectedPersonHoursForOutput(
                production, process, standardPersonHoursRaw);
        BigDecimal hourlyCost = production.getLaborHourlyCostSnapshot();
        BigDecimal processExpectedLaborRaw = multiply(expectedPersonHoursRaw, hourlyCost);
        BigDecimal recipeExpectedLaborRaw = nonPercentageCost(
                production, AdditionalCostType.LABOR, production.getFinalUnits());
        BigDecimal expectedLaborRaw = hasType(production, AdditionalCostType.LABOR)
                ? recipeExpectedLaborRaw : processExpectedLaborRaw;
        BigDecimal actualLaborRaw = multiply(actualPersonHoursRaw, hourlyCost);
        BigDecimal laborDeviationRaw = subtract(actualLaborRaw, expectedLaborRaw);

        BigDecimal expectedPackagingRaw = nonPercentageCost(
                production, AdditionalCostType.PACKAGING, production.getFinalUnits());
        BigDecimal actualPackagingRaw = expectedPackagingRaw;
        BigDecimal expectedOtherNonPercentageRaw = nonPercentageCost(
                production, AdditionalCostType.OTHER, production.getFinalUnits());
        BigDecimal actualOtherNonPercentageRaw = expectedOtherNonPercentageRaw;

        BigDecimal expectedPercentageBase = sum(expectedIngredientRaw, expectedLaborRaw,
                expectedPackagingRaw, expectedOtherNonPercentageRaw);
        BigDecimal actualPercentageBase = sum(actualIngredientRaw, actualLaborRaw,
                actualPackagingRaw, actualOtherNonPercentageRaw);
        BigDecimal energyPercentage = percentageFor(
                production, AdditionalCostType.ENERGY, production.getEnergyPercentageSnapshot());
        BigDecimal expectedEnergyRaw = percentageCost(expectedPercentageBase, energyPercentage);
        BigDecimal actualEnergyRaw = percentageCost(actualPercentageBase, energyPercentage);
        BigDecimal otherPercentage = percentageFor(
                production, AdditionalCostType.OTHER, BigDecimal.ZERO);
        BigDecimal expectedOtherPercentageRaw = percentageCost(expectedPercentageBase, otherPercentage);
        BigDecimal actualOtherPercentageRaw = percentageCost(actualPercentageBase, otherPercentage);
        BigDecimal expectedOtherRaw = sum(expectedOtherNonPercentageRaw, expectedOtherPercentageRaw);
        BigDecimal actualOtherRaw = sum(actualOtherNonPercentageRaw, actualOtherPercentageRaw);

        BigDecimal standardTotalRaw = sum(expectedIngredientRaw, expectedLaborRaw,
                expectedPackagingRaw, expectedOtherRaw, expectedEnergyRaw);
        BigDecimal actualTotalRaw = sum(actualIngredientRaw, actualLaborRaw,
                actualPackagingRaw, actualOtherRaw, actualEnergyRaw);
        BigDecimal actualCostPerUnitRaw = perUnit(actualTotalRaw, production.getFinalUnits());
        BigDecimal standardCostPerUnitRaw = perUnit(standardTotalRaw, production.getFinalUnits());
        BigDecimal totalDeviationRaw = subtract(actualTotalRaw, standardTotalRaw);
        BigDecimal deviationPerUnitRaw = subtract(actualCostPerUnitRaw, standardCostPerUnitRaw);
        BigDecimal estimatedWasteCostRaw = production.getWasteUnits() == null ? null
                : production.getWasteUnits() == 0 ? BigDecimal.ZERO
                : multiply(actualCostPerUnitRaw, BigDecimal.valueOf(production.getWasteUnits()));

        return new ProductionCostDetailDTO(
                production.getId(), production.getProductionDate(),
                production.getVariety().getVarietyId(), production.getVariety().getName(),
                production.getPlannedUnits(), production.getFinalUnits(), production.getWasteUnits(),
                percentage(production.getWasteUnits(), production.getPlannedUnits()),
                production.getTotalMinutes(), production.getPeopleCount(),
                money(expectedIngredientRaw), money(actualIngredientRaw),
                money(actualIngredientRaw.subtract(expectedIngredientRaw)),
                metric(standardUnitsPerHourRaw), metric(actualUnitsPerHourRaw),
                metric(productivityVariationRaw), metric(standardPersonHoursRaw),
                metric(actualPersonHoursRaw), metric(standardUnitsPerPersonHourRaw),
                metric(actualUnitsPerPersonHourRaw), metric(laborVariationRaw), money(hourlyCost),
                metric(expectedPersonHoursRaw), money(expectedLaborRaw), money(actualLaborRaw),
                money(laborDeviationRaw), money(expectedPackagingRaw), money(actualPackagingRaw),
                money(subtract(actualPackagingRaw, expectedPackagingRaw)), money(expectedOtherRaw),
                money(actualOtherRaw), money(subtract(actualOtherRaw, expectedOtherRaw)),
                money(expectedEnergyRaw), money(actualEnergyRaw),
                money(subtract(actualEnergyRaw, expectedEnergyRaw)), money(standardTotalRaw),
                money(actualTotalRaw), money(standardCostPerUnitRaw), money(actualCostPerUnitRaw),
                money(totalDeviationRaw), money(deviationPerUnitRaw), money(estimatedWasteCostRaw),
                performanceStatus(laborVariationRaw));
    }

    private boolean hasType(Production production, AdditionalCostType type) {
        return production.getAdditionalCosts().stream().anyMatch(item -> item.getCostType() == type);
    }

    private BigDecimal nonPercentageCost(
            Production production, AdditionalCostType type, Integer unitsValue) {
        if (unitsValue == null) return null;
        BigDecimal units = BigDecimal.valueOf(unitsValue);
        BigDecimal baseYield = BigDecimal.valueOf(production.getRecipe().getBaseYieldUnits());
        BigDecimal result = BigDecimal.ZERO;
        for (ProductionAdditionalCost item : production.getAdditionalCosts()) {
            if (item.getCostType() != type
                    || item.getCalculationModeSnapshot() == AdditionalCostCalculationMode.PERCENTAGE) {
                continue;
            }
            if (item.getCalculationModeSnapshot() == AdditionalCostCalculationMode.FIXED_TOTAL) {
                result = result.add(item.getValueSnapshot().multiply(units)
                        .divide(baseYield, CALCULATION_SCALE, RoundingMode.HALF_UP));
            } else {
                result = result.add(item.getValueSnapshot().multiply(units));
            }
        }
        return result;
    }

    private BigDecimal percentageFor(
            Production production, AdditionalCostType type, BigDecimal fallback) {
        BigDecimal result = production.getAdditionalCosts().stream()
                .filter(item -> item.getCostType() == type
                        && item.getCalculationModeSnapshot() == AdditionalCostCalculationMode.PERCENTAGE)
                .map(ProductionAdditionalCost::getValueSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return result.signum() == 0 ? fallback : result;
    }

    private BigDecimal percentageCost(BigDecimal base, BigDecimal percentage) {
        if (base == null || percentage == null) return null;
        return base.multiply(percentage)
                .divide(ONE_HUNDRED, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal standardUnitsPerHour(ProductionProcess process) {
        if (process == null || process.getReferenceYieldUnits() == null) return null;
        int totalMinutes = process.getSteps().stream().mapToInt(step -> step.getEstimatedMinutes()).sum();
        if (totalMinutes <= 0) return null;
        return BigDecimal.valueOf(process.getReferenceYieldUnits()).multiply(SIXTY)
                .divide(BigDecimal.valueOf(totalMinutes), CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal actualUnitsPerHour(Production production) {
        if (production.getFinalUnits() == null || production.getTotalMinutes() == null
                || production.getTotalMinutes() <= 0) return null;
        return BigDecimal.valueOf(production.getFinalUnits()).multiply(SIXTY)
                .divide(BigDecimal.valueOf(production.getTotalMinutes()),
                        CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal standardPersonHours(ProductionProcess process) {
        if (process == null) return null;
        long personMinutes = process.getSteps().stream()
                .mapToLong(step -> (long) step.getEstimatedMinutes() * step.getRequiredPeople()).sum();
        if (personMinutes <= 0) return null;
        return BigDecimal.valueOf(personMinutes).divide(SIXTY, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal actualPersonHours(Production production) {
        if (production.getTotalMinutes() == null || production.getPeopleCount() == null
                || production.getTotalMinutes() <= 0 || production.getPeopleCount() <= 0) return null;
        return BigDecimal.valueOf(production.getTotalMinutes())
                .multiply(BigDecimal.valueOf(production.getPeopleCount()))
                .divide(SIXTY, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal expectedPersonHoursForOutput(
            Production production, ProductionProcess process, BigDecimal standardPersonHours) {
        if (production.getFinalUnits() == null || process == null
                || process.getReferenceYieldUnits() == null || process.getReferenceYieldUnits() <= 0
                || standardPersonHours == null) return null;
        return BigDecimal.valueOf(production.getFinalUnits()).multiply(standardPersonHours)
                .divide(BigDecimal.valueOf(process.getReferenceYieldUnits()),
                        CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal unitsPerHour(Integer units, BigDecimal hours) {
        if (units == null || hours == null || hours.signum() <= 0) return null;
        return BigDecimal.valueOf(units).divide(hours, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal variation(BigDecimal actual, BigDecimal standard) {
        if (actual == null || standard == null || standard.signum() <= 0) return null;
        return actual.divide(standard, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE).multiply(ONE_HUNDRED);
    }

    private BigDecimal perUnit(BigDecimal total, Integer units) {
        if (total == null || units == null || units <= 0) return null;
        return total.divide(BigDecimal.valueOf(units), CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator <= 0) return null;
        return metric(BigDecimal.valueOf(numerator).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), CALCULATION_SCALE, RoundingMode.HALF_UP));
    }

    private String performanceStatus(BigDecimal laborVariation) {
        if (laborVariation == null) return null;
        if (laborVariation.compareTo(new BigDecimal("15")) >= 0) return "EXCELLENT";
        if (laborVariation.compareTo(new BigDecimal("5")) >= 0) return "GOOD";
        if (laborVariation.compareTo(new BigDecimal("-5")) >= 0) return "NORMAL";
        if (laborVariation.compareTo(new BigDecimal("-20")) >= 0) return "WARNING";
        return "CRITICAL";
    }

    private BigDecimal actualQuantity(ProductionIngredient item) {
        return item.getActualQuantity() == null ? item.getExpectedQuantity() : item.getActualQuantity();
    }

    private BigDecimal multiply(BigDecimal first, BigDecimal second) {
        return first == null || second == null ? null : first.multiply(second);
    }

    private BigDecimal subtract(BigDecimal first, BigDecimal second) {
        return first == null || second == null ? null : first.subtract(second);
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value == null) return null;
            result = result.add(value);
        }
        return result;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal metric(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
