package com.bienCriollas.stock.production.ingredient.exception;

import java.math.BigDecimal;
import com.bienCriollas.stock.production.ingredient.enums.MeasurementUnit;

public class InsufficientIngredientStockException extends RuntimeException {
    public InsufficientIngredientStockException(
            String name, BigDecimal available, BigDecimal requested, MeasurementUnit unit) {
        super("Stock insuficiente de " + name + ". Disponible: " + available.toPlainString()
                + " " + unit.symbol() + ". Solicitado: " + requested.toPlainString()
                + " " + unit.symbol() + ".");
    }
}
