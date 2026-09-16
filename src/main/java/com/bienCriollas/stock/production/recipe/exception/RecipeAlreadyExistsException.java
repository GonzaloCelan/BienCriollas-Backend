package com.bienCriollas.stock.production.recipe.exception;

public class RecipeAlreadyExistsException extends RuntimeException {
    public RecipeAlreadyExistsException(String varietyName) {
        super("La variedad " + varietyName + " ya tiene una receta activa.");
    }
}

