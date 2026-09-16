package com.bienCriollas.stock.production.recipe.exception;

public class RecipeWithoutIngredientsException extends RuntimeException {
    public RecipeWithoutIngredientsException() {
        super("La receta debe contener al menos un ingrediente.");
    }
}

