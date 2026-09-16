package com.bienCriollas.stock.production.ingredient.exception;

import java.math.BigDecimal;

public class InsufficientIngredientStockException extends RuntimeException {
    public InsufficientIngredientStockException(String name, BigDecimal available, BigDecimal requested) {
        super("Stock insuficiente de " + name + ". Disponible: " + available.toPlainString()
                + " g. Solicitado: " + requested.toPlainString() + " g.");
    }
}

