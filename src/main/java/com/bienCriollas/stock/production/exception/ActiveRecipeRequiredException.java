package com.bienCriollas.stock.production.exception;

public class ActiveRecipeRequiredException extends RuntimeException {
    public ActiveRecipeRequiredException(String varietyName) {
        super("La variedad " + varietyName + " no posee una receta vigente.");
    }
}
