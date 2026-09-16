package com.bienCriollas.stock.production.recipe.exception;

public class InvalidRecipeYieldException extends RuntimeException {
    public InvalidRecipeYieldException() {
        super("El rendimiento de la receta debe ser mayor a 0.");
    }
}

