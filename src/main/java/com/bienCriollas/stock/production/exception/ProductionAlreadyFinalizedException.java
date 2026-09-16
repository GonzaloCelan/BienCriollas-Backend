package com.bienCriollas.stock.production.exception;

public class ProductionAlreadyFinalizedException extends RuntimeException {
    public ProductionAlreadyFinalizedException() {
        super("La producción ya fue finalizada.");
    }
}
