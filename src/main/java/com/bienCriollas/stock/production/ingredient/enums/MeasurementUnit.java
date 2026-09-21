package com.bienCriollas.stock.production.ingredient.enums;

public enum MeasurementUnit {
    GRAM("g"),
    MILLILITER("ml"),
    UNIT("u");

    private final String symbol;

    MeasurementUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
