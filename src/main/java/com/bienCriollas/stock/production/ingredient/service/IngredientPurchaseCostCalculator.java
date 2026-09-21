package com.bienCriollas.stock.production.ingredient.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.bienCriollas.stock.production.ingredient.exception.InvalidIngredientException;

@Component
public class IngredientPurchaseCostCalculator {

    public static final int COST_SCALE = 6;
    private static final RoundingMode COST_ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal calculate(BigDecimal purchasePrice, BigDecimal purchaseQuantity) {
        if (purchaseQuantity == null || purchaseQuantity.signum() <= 0) {
            throw new InvalidIngredientException(
                    "La cantidad de la presentación de compra debe ser mayor a cero.");
        }
        if (purchasePrice == null || purchasePrice.signum() <= 0) {
            throw new InvalidIngredientException(
                    "El precio de la presentación de compra debe ser mayor a cero.");
        }
        BigDecimal calculatedCost = purchasePrice.divide(
                purchaseQuantity, COST_SCALE, COST_ROUNDING);
        if (calculatedCost.signum() <= 0) {
            throw new InvalidIngredientException(
                    "El costo por unidad base calculado es menor que la precisión admitida.");
        }
        return calculatedCost;
    }
}
