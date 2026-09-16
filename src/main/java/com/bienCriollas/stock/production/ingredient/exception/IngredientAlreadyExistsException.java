package com.bienCriollas.stock.production.ingredient.exception;

public class IngredientAlreadyExistsException extends RuntimeException {
    public IngredientAlreadyExistsException(String name) {
        super("Ya existe un ingrediente llamado '" + name + "'.");
    }
}

