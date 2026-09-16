package com.bienCriollas.stock.production.recipe.exception;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(Long id) {
        super("Receta con id " + id + " no encontrada.");
    }

    public static RecipeNotFoundException forVariety(Long varietyId) {
        return new RecipeNotFoundException(
                "No existe una receta activa para la variedad con id " + varietyId + ".");
    }

    private RecipeNotFoundException(String message) {
        super(message);
    }
}

