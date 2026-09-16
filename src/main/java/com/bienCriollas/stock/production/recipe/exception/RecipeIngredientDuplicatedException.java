package com.bienCriollas.stock.production.recipe.exception;

public class RecipeIngredientDuplicatedException extends RuntimeException {
    public RecipeIngredientDuplicatedException(String ingredientName) {
        super("El ingrediente " + ingredientName + " está repetido dentro de la receta.");
    }
}

