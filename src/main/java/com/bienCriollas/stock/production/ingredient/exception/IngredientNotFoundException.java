package com.bienCriollas.stock.production.ingredient.exception;

public class IngredientNotFoundException extends RuntimeException {
    public IngredientNotFoundException(Long id) {
        super("Ingrediente con id " + id + " no encontrado.");
    }
}

