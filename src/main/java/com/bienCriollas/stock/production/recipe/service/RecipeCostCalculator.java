package com.bienCriollas.stock.production.recipe.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.production.recipe.dto.*;
import com.bienCriollas.stock.production.recipe.entity.*;
import com.bienCriollas.stock.production.recipe.enums.AdditionalCostCalculationMode;

@Component
public class RecipeCostCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int INTERNAL_SCALE = 10;

    public Calculation calculate(Recipe recipe, int requestedUnits, BigDecimal ingredientCost) {
        BigDecimal units = BigDecimal.valueOf(requestedUnits);
        BigDecimal baseYield = BigDecimal.valueOf(recipe.getBaseYieldUnits());
        BigDecimal factor = units.divide(baseYield, INTERNAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal fixed = BigDecimal.ZERO;
        BigDecimal perUnit = BigDecimal.ZERO;
        for (RecipeAdditionalCost item : recipe.getAdditionalCosts()) {
            if (!Boolean.TRUE.equals(item.getActive())) continue;
            if (item.getCalculationMode() == AdditionalCostCalculationMode.FIXED_TOTAL) {
                fixed = fixed.add(item.getValue().multiply(factor));
            } else if (item.getCalculationMode() == AdditionalCostCalculationMode.PER_UNIT) {
                perUnit = perUnit.add(item.getValue().multiply(units));
            }
        }

        BigDecimal subtotal = ingredientCost.add(fixed).add(perUnit);
        BigDecimal percentage = BigDecimal.ZERO;
        List<RecipeAdditionalCostResponseDTO> costs = new ArrayList<>();
        Map<Long, BigDecimal> rawCostsById = new LinkedHashMap<>();
        for (RecipeAdditionalCost item : recipe.getAdditionalCosts()) {
            if (!Boolean.TRUE.equals(item.getActive())) continue;
            BigDecimal calculated = switch (item.getCalculationMode()) {
                case FIXED_TOTAL -> item.getValue().multiply(factor);
                case PER_UNIT -> item.getValue().multiply(units);
                case PERCENTAGE -> subtotal.multiply(item.getValue())
                        .divide(ONE_HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP);
            };
            if (item.getCalculationMode() == AdditionalCostCalculationMode.PERCENTAGE) {
                percentage = percentage.add(calculated);
            }
            rawCostsById.put(item.getId(), calculated);
            costs.add(new RecipeAdditionalCostResponseDTO(
                    item.getId(), item.getCostType(), item.getName(), item.getCalculationMode(),
                    item.getValue(), money(calculated), item.getSortOrder(), item.getNotes()));
        }

        BigDecimal additional = fixed.add(perUnit).add(percentage);
        BigDecimal total = ingredientCost.add(additional);
        BigDecimal perUnitCost = total.divide(units, INTERNAL_SCALE, RoundingMode.HALF_UP);
        RecipeCostSummaryDTO summary = new RecipeCostSummaryDTO(
                money(ingredientCost), money(fixed), money(perUnit), money(subtotal),
                money(percentage), money(additional), money(total), money(perUnitCost));
        return new Calculation(List.copyOf(costs), summary, Map.copyOf(rawCostsById));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record Calculation(
            List<RecipeAdditionalCostResponseDTO> additionalCosts,
            RecipeCostSummaryDTO summary,
            Map<Long, BigDecimal> rawAdditionalCostsById) {}
}
