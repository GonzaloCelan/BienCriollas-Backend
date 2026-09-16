package com.bienCriollas.stock.production.ingredient.exception;

public class IngredientInactiveException extends RuntimeException {
    public IngredientInactiveException(Long id) {
        super("El ingrediente con id " + id + " está inactivo.");
    }
}

