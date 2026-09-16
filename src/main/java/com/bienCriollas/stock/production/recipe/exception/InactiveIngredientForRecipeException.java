package com.bienCriollas.stock.production.recipe.exception;

public class InactiveIngredientForRecipeException extends RuntimeException {
    public InactiveIngredientForRecipeException(String ingredientName) {
        super("El ingrediente " + ingredientName
                + " está inactivo y no puede utilizarse en una receta nueva.");
    }
}

