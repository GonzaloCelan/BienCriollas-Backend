package com.bienCriollas.stock.production.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.production.analytics.dto.ProductionCostDetailDTO;
import com.bienCriollas.stock.production.entity.Production;
import com.bienCriollas.stock.production.entity.ProductionIngredient;
import com.bienCriollas.stock.production.process.entity.ProductionProcess;

@Component
public class ProductionCostCalculator {

    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CALCULATION_SCALE = 10;

    public ProductionCostDetailDTO calculate(Production production) {
        BigDecimal expectedIngredientCostRaw = production.getIngredients().stream()
                .map(item -> item.getExpectedQuantityGrams().multiply(item.getCostPerGramSnapshot()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualIngredientCostRaw = production.getIngredients().stream()
                .map(item -> actualQuantity(item).multiply(item.getCostPerGramSnapshot()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedIngredientCost = money(expectedIngredientCostRaw);
        BigDecimal actualIngredientCost = money(actualIngredientCostRaw);
        BigDecimal ingredientDeviation = money(actualIngredientCostRaw.subtract(expectedIngredientCostRaw));

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
        BigDecimal expectedLaborCostRaw = multiply(expectedPersonHoursRaw, hourlyCost);
        BigDecimal actualLaborCostRaw = multiply(actualPersonHoursRaw, hourlyCost);
        BigDecimal laborInefficiencyRaw = subtract(actualLaborCostRaw, expectedLaborCostRaw);

        BigDecimal energyPercentage = production.getEnergyPercentageSnapshot();
        BigDecimal actualEnergyCostRaw = energyCost(
                actualIngredientCostRaw, actualLaborCostRaw, energyPercentage);
        BigDecimal standardEnergyCostRaw = energyCost(
                expectedIngredientCostRaw, expectedLaborCostRaw, energyPercentage);
        BigDecimal actualTotalRaw = sum(actualIngredientCostRaw, actualLaborCostRaw, actualEnergyCostRaw);
        BigDecimal standardTotalRaw = sum(
                expectedIngredientCostRaw, expectedLaborCostRaw, standardEnergyCostRaw);
        BigDecimal actualCostPerUnitRaw = perUnit(actualTotalRaw, production.getFinalUnits());
        BigDecimal standardCostPerUnitRaw = perUnit(standardTotalRaw, production.getFinalUnits());
        BigDecimal totalDeviationRaw = subtract(actualTotalRaw, standardTotalRaw);
        BigDecimal deviationPerUnitRaw = subtract(actualCostPerUnitRaw, standardCostPerUnitRaw);
        BigDecimal estimatedWasteCostRaw = production.getWasteUnits() == null
                ? null
                : production.getWasteUnits() == 0
                        ? BigDecimal.ZERO
                        : multiply(actualCostPerUnitRaw,
                                BigDecimal.valueOf(production.getWasteUnits()));

        return new ProductionCostDetailDTO(
                production.getId(),
                production.getProductionDate(),
                production.getVariety().getVarietyId(),
                production.getVariety().getName(),
                production.getPlannedUnits(),
                production.getFinalUnits(),
                production.getWasteUnits(),
                percentage(production.getWasteUnits(), production.getPlannedUnits()),
                production.getTotalMinutes(),
                production.getPeopleCount(),
                expectedIngredientCost,
                actualIngredientCost,
                ingredientDeviation,
                metric(standardUnitsPerHourRaw),
                metric(actualUnitsPerHourRaw),
                metric(productivityVariationRaw),
                metric(standardPersonHoursRaw),
                metric(actualPersonHoursRaw),
                metric(standardUnitsPerPersonHourRaw),
                metric(actualUnitsPerPersonHourRaw),
                metric(laborVariationRaw),
                money(hourlyCost),
                metric(expectedPersonHoursRaw),
                money(expectedLaborCostRaw),
                money(actualLaborCostRaw),
                money(laborInefficiencyRaw),
                money(actualEnergyCostRaw),
                money(standardTotalRaw),
                money(actualTotalRaw),
                money(standardCostPerUnitRaw),
                money(actualCostPerUnitRaw),
                money(totalDeviationRaw),
                money(deviationPerUnitRaw),
                money(estimatedWasteCostRaw),
                performanceStatus(laborVariationRaw));
    }

    private BigDecimal standardUnitsPerHour(ProductionProcess process) {
        if (process == null || process.getReferenceYieldUnits() == null) return null;
        int totalMinutes = process.getSteps().stream()
                .mapToInt(step -> step.getEstimatedMinutes()).sum();
        if (totalMinutes <= 0) return null;
        return BigDecimal.valueOf(process.getReferenceYieldUnits()).multiply(SIXTY)
                .divide(BigDecimal.valueOf(totalMinutes), CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal actualUnitsPerHour(Production production) {
        if (production.getFinalUnits() == null
                || production.getTotalMinutes() == null
                || production.getTotalMinutes() <= 0) return null;
        return BigDecimal.valueOf(production.getFinalUnits()).multiply(SIXTY)
                .divide(BigDecimal.valueOf(production.getTotalMinutes()),
                        CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal standardPersonHours(ProductionProcess process) {
        if (process == null) return null;
        long personMinutes = process.getSteps().stream()
                .mapToLong(step -> (long) step.getEstimatedMinutes() * step.getRequiredPeople())
                .sum();
        if (personMinutes <= 0) return null;
        return BigDecimal.valueOf(personMinutes)
                .divide(SIXTY, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal actualPersonHours(Production production) {
        if (production.getTotalMinutes() == null
                || production.getPeopleCount() == null
                || production.getTotalMinutes() <= 0
                || production.getPeopleCount() <= 0) return null;
        return BigDecimal.valueOf(production.getTotalMinutes())
                .multiply(BigDecimal.valueOf(production.getPeopleCount()))
                .divide(SIXTY, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal expectedPersonHoursForOutput(
            Production production,
            ProductionProcess process,
            BigDecimal standardPersonHours) {
        if (production.getFinalUnits() == null
                || process == null
                || process.getReferenceYieldUnits() == null
                || process.getReferenceYieldUnits() <= 0
                || standardPersonHours == null) return null;
        return BigDecimal.valueOf(production.getFinalUnits())
                .multiply(standardPersonHours)
                .divide(BigDecimal.valueOf(process.getReferenceYieldUnits()),
                        CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal unitsPerHour(Integer units, BigDecimal hours) {
        if (units == null || hours == null || hours.signum() <= 0) return null;
        return BigDecimal.valueOf(units)
                .divide(hours, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal variation(BigDecimal actual, BigDecimal standard) {
        if (actual == null || standard == null || standard.signum() <= 0) return null;
        return actual.divide(standard, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(ONE_HUNDRED);
    }

    private BigDecimal energyCost(
            BigDecimal ingredientCost,
            BigDecimal laborCost,
            BigDecimal energyPercentage) {
        if (ingredientCost == null || laborCost == null || energyPercentage == null) return null;
        return ingredientCost.add(laborCost).multiply(energyPercentage)
                .divide(ONE_HUNDRED, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal perUnit(BigDecimal total, Integer units) {
        if (total == null || units == null || units <= 0) return null;
        return total.divide(BigDecimal.valueOf(units), CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator <= 0) return null;
        return metric(BigDecimal.valueOf(numerator).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator),
                        CALCULATION_SCALE, RoundingMode.HALF_UP));
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
        return item.getActualQuantityGrams() == null
                ? item.getExpectedQuantityGrams()
                : item.getActualQuantityGrams();
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
